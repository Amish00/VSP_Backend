package com.final_year.v2.service;

import com.final_year.v2.constaint.Plan;
import com.final_year.v2.dto.PaymentInitiateRequest;
import com.final_year.v2.model.*;
import com.final_year.v2.repository.*;
import com.final_year.v2.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RevenueSubscriptionRepository revenueSubscriptionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private RevenueRecordRepository revenueRecordRepository;

    @Value("${esewa.merchant.code}")
    private String esewaMerchantCode;

    @Value("${esewa.secret.key}")
    private String esewaSecretKey;

    @Value("${esewa.base.url}")
    private String esewaBaseUrl;

    @Value("${khalti.secret.key}")
    private String khaltiSecretKey;

    @Value("${khalti.base.url}")
    private String khaltiBaseUrl;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, UserRepository userRepository,
                          UserService userService, RevenueSubscriptionRepository revenueSubscriptionRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.revenueSubscriptionRepository = revenueSubscriptionRepository;
    }

    // =========================
    //  Authentication helper
    // =========================
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        String email;
        if (principal instanceof UserDetailsImpl userDetails) {
            email = userDetails.getEmail().toLowerCase();
        } else if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername().toLowerCase();
        } else {
            email = String.valueOf(principal).toLowerCase();
        }
        if ("anonymoususer".equals(email)) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));
        return user.getId();
    }

    // =========================
    //  Initiate payment (eSewa / Khalti)
    // =========================
    @Transactional
    public Map<String, Object> initiatePayment(PaymentInitiateRequest request) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSubscriptionExpiry() != null && user.getSubscriptionExpiry().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("You already have an active " + user.getPlan() +
                    " plan until " + user.getSubscriptionExpiry());
        }

        String transactionId = UUID.randomUUID().toString();
        Payment payment = new Payment();
        payment.setTransactionId(transactionId);
        payment.setGateway(request.getGateway());
        payment.setAmount(request.getAmount());
        payment.setStatus("PENDING");
        payment.setUserId(userId);
        payment.setPlanId(request.getPlanId());
        payment.setBillingCycle(request.getBillingCycle());
        paymentRepository.save(payment);

        if ("esewa".equalsIgnoreCase(request.getGateway())) {
            return buildEsewaPayload(payment);
        } else if ("khalti".equalsIgnoreCase(request.getGateway())) {
            return buildKhaltiPayload(payment);
        }
        throw new RuntimeException("Unsupported gateway: " + request.getGateway());
    }

    // eSewa payload builder (unchanged)
    private Map<String, Object> buildEsewaPayload(Payment payment) {
        String amount = payment.getAmount().setScale(2, RoundingMode.HALF_UP).toString();
        String transactionUuid = payment.getTransactionId();
        String productCode = esewaMerchantCode;
        String signedFieldNames = "total_amount,transaction_uuid,product_code";
        String dataToSign = "total_amount=" + amount + ",transaction_uuid=" + transactionUuid + ",product_code=" + productCode;
        String signature = generateHmacSha256(dataToSign, esewaSecretKey);

        Map<String, String> params = new HashMap<>();
        params.put("amount", amount);
        params.put("tax_amount", "0");
        params.put("total_amount", amount);
        params.put("transaction_uuid", transactionUuid);
        params.put("product_code", productCode);
        params.put("product_service_charge", "0");
        params.put("product_delivery_charge", "0");
        params.put("success_url", "http://localhost:8080/api/payment/callback/esewa");
        params.put("failure_url", "http://localhost:8080/api/payment/callback/esewa/failure");
        params.put("signed_field_names", signedFieldNames);
        params.put("signature", signature);
        return Map.of("gateway", "esewa", "url", esewaBaseUrl, "params", params);
    }

    // Khalti payload builder (unchanged)
    private Map<String, Object> buildKhaltiPayload(Payment payment) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + khaltiSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("return_url", "http://localhost:8080/api/payment/callback/khalti");
        body.put("website_url", "http://localhost:5173");
        body.put("amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
        body.put("purchase_order_id", payment.getTransactionId());
        body.put("purchase_order_name", "Video Plan Subscription");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                khaltiBaseUrl + "/epayment/initiate/",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("pidx")) {
            throw new RuntimeException("Khalti initiate response missing pidx");
        }
        String pidx = (String) responseBody.get("pidx");
        payment.setPidx(pidx);
        paymentRepository.save(payment);
        return Map.of("gateway", "khalti", "payment_url", responseBody.get("payment_url"));
    }

    // eSewa callback verification (unchanged except for upgrade call)
    @Transactional
    public void verifyEsewa(String encodedData, boolean isSuccess) {
        if (!isSuccess) return;
        try {
            String decodedJson = new String(Base64.getDecoder().decode(encodedData), StandardCharsets.UTF_8);
            String transactionUuid = extractValue(decodedJson, "transaction_uuid");
            String totalAmount = extractValue(decodedJson, "total_amount");
            Payment payment = paymentRepository.findByTransactionId(transactionUuid)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
            if (!"PENDING".equals(payment.getStatus())) return;

            BigDecimal paidAmount = new BigDecimal(totalAmount);
            if (paidAmount.compareTo(payment.getAmount()) != 0) {
                payment.setStatus("FAILED");
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                return;
            }
            payment.setStatus("SUCCESS");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            upgradeUserPlan(payment);
        } catch (Exception e) {
            throw new RuntimeException("eSewa verification failed", e);
        }
    }

    // Khalti callback verification (unchanged)
    @Transactional
    public void verifyKhalti(String pidx) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + khaltiSecretKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                khaltiBaseUrl + "/epayment/lookup/?pidx=" + pidx,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("status")) {
            throw new RuntimeException("Khalti lookup response invalid");
        }
        String status = (String) body.get("status");
        Payment payment = paymentRepository.findByPidx(pidx)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        if (!"PENDING".equals(payment.getStatus())) return;
        if ("Completed".equalsIgnoreCase(status)) {
            payment.setStatus("SUCCESS");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            upgradeUserPlan(payment);
        } else {
            payment.setStatus("FAILED");
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
    }

    // Modified: now creates RevenueSubscription record
    private void upgradeUserPlan(Payment payment) {
        User user = userRepository.findById(payment.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Plan newPlan = "creator".equalsIgnoreCase(payment.getPlanId()) ? Plan.CREATE : Plan.VIEW;
        LocalDateTime expiry = LocalDateTime.now();
        int months;
        switch (payment.getBillingCycle().toLowerCase()) {
            case "monthly":
                expiry = expiry.plusMonths(1);
                months = 1;
                break;
            case "half":
                expiry = expiry.plusMonths(6);
                months = 6;
                break;
            case "yearly":
                expiry = expiry.plusYears(1);
                months = 12;
                break;
            default:
                throw new RuntimeException("Invalid billing cycle");
        }
        user.setPlan(newPlan);
        user.setSubscriptionExpiry(expiry);
        userService.syncRoleFromPlan(user);
        userRepository.save(user);

        // Create revenue subscription record
        BigDecimal monthlyAmount = payment.getAmount().divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        RevenueSubscription rs = new RevenueSubscription();
        rs.setUserId(user.getId());
        rs.setPlanType(payment.getPlanId());
        rs.setBillingCycle(payment.getBillingCycle());
        rs.setTotalAmount(payment.getAmount());
        rs.setMonthlyAmount(monthlyAmount);
        rs.setStartDate(LocalDateTime.now());
        rs.setEndDate(expiry);
        revenueSubscriptionRepository.save(rs);

        // Create revenue record for admin table
        RevenueRecord revenueRecord = new RevenueRecord();
        revenueRecord.setUser(user);
        revenueRecord.setUsername(user.getUsername());
        revenueRecord.setPaymentMethod(payment.getGateway());
        revenueRecord.setTransactionId(payment.getTransactionId());
        revenueRecord.setAmount(payment.getAmount());
        revenueRecord.setPlanType(payment.getPlanId());
        revenueRecord.setBillingCycle(payment.getBillingCycle());
        revenueRecord.setTransactionDate(payment.getCreatedAt());
        revenueRecord.setSubscriptionStartDate(LocalDateTime.now());
        revenueRecord.setSubscriptionEndDate(expiry);
        revenueRecordRepository.save(revenueRecord);
    }

    // Free plan upgrade (unchanged)
    @Transactional
    public void upgradeToFreePlan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPlan(Plan.FREE);
        user.setSubscriptionExpiry(null);
        userService.syncRoleFromPlan(user);
        userRepository.save(user);
    }

    // Helpers
    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String generateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }
}