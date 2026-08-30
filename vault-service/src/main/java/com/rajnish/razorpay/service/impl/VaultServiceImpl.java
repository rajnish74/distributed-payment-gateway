package com.rajnish.razorpay.service.impl;


import com.rajnish.razorpay.config.VaultEncryption;
import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.dto.request.TokenizeRequest;
import com.rajnish.razorpay.dto.response.TokenizeResponse;
import com.rajnish.razorpay.entity.CardToken;
import com.rajnish.razorpay.entity.Money;
import com.rajnish.razorpay.entity.VaultCard;
import com.rajnish.razorpay.enums.CardBrand;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.processor.CardPaymentProcessor;
import com.rajnish.razorpay.repository.CardTokenRepository;
import com.rajnish.razorpay.repository.VaultCardRepository;
import com.rajnish.razorpay.service.VaultService;
import com.rajnish.razorpay.utils.RandomizerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class VaultServiceImpl implements VaultService {

    private final CardTokenRepository cardTokenRepository;
    private final VaultCardRepository vaultCardRepository;
    private final BytesEncryptor dekEncryptor;
    private final CardPaymentProcessor  cardPaymentProcessor;

    @Override
    @Transactional
    public TokenizeResponse tokenize(TokenizeRequest request, UUID merchantId) {
        String lastFour=request.pan().substring(request.pan().length()-4);
        String bin=request.pan().substring(0,6);
        CardBrand cardBrand=detectBrand(request.pan());

        byte[] dek= KeyGenerators.secureRandom(32).generateKey();
        byte[] encryptedPan= VaultEncryption.panEncrypt(dek)
                .encrypt(request.pan().getBytes(StandardCharsets.UTF_8));
        byte[] encryptedDek=dekEncryptor.encrypt(dek);

        VaultCard card=vaultCardRepository.save(VaultCard.builder()
                .brand(cardBrand)
                .expiryMonth(request.expiryMonth().toString())
                .expiryYear(request.expiryYear().toString())
                .lastFourDigits(lastFour)
                .bin(bin)
                .encryptedPan(encryptedPan)
                .encryptedDek(encryptedDek)
                .cardholderName(request.cardHolderName())
                .build());

        String token="tok_"+ RandomizerUtil.randomBase64(32);

        cardTokenRepository.save(CardToken.builder()
                        .vaultCard(card)
                        .token(token)
                        .customer(request.customerId())
                        .merchant(merchantId)
                .build());


        return new TokenizeResponse(token,lastFour,cardBrand, request.expiryMonth(), request.expiryYear());
    }

    @Override
    @Transactional
    public PaymentProcessorResponse charge(UUID paymentId, String token, Money amount, Map<String, Object> methodDetails) {

        CardToken cardToken=cardTokenRepository.findByTokenAndRevokedAtIsNull(token)
                .orElseThrow(()-> new ResourceNotFoundException("CardToken ",token));

        VaultCard vaultCard=cardToken.getVaultCard();
        byte[] panBytes=null;


        try {
            byte[] dek = dekEncryptor.decrypt(vaultCard.getEncryptedDek());
            panBytes = VaultEncryption.panEncrypt(dek).decrypt(vaultCard.getEncryptedPan());

            String pan = new String(panBytes, StandardCharsets.UTF_8);
            String expiry = vaultCard.getExpiryMonth() + "/" + vaultCard.getExpiryYear();

            com.rajnish.razorpay.dto.PaymentProcessorRequest paymentProcessorRequest = com.rajnish.razorpay.dto.PaymentProcessorRequest.card(
                    paymentId,
                    pan,
                    expiry,
                    amount,
                    methodDetails
            );

            PaymentProcessorResponse response = cardPaymentProcessor.charge(paymentProcessorRequest);

            log.info("Vault charge registered, token={}****", token.substring(0, 4));



            return response;
        } catch (Exception e) {
            log.warn("Vault charge failed, token={}****", token.substring(0, 4));
            return new PaymentProcessorResponse.Failure("VAULT_CHARGE_FAILED", e.getMessage());
        }finally {
            if (panBytes != null) Arrays.fill(panBytes, (byte) 0);
        }
    }

    private CardBrand detectBrand(String pan) {
        if (pan.startsWith("4")) {
            return CardBrand.VISA;
        }
        if (pan.startsWith("5")||pan.startsWith("2")) {
            return CardBrand.MASTERCARD;
        }
        if (pan.startsWith("37") || pan.startsWith("34")) {
            return CardBrand.AMEX;
        }
        return CardBrand.RUPAY;

    }
}
