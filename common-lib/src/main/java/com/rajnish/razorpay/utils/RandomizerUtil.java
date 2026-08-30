package com.rajnish.razorpay.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomizerUtil {

    private static final SecureRandom SECURE_RANDOM=new SecureRandom();

    public static String randomBase64(int length){
        byte[] buff=new byte[length];
        SECURE_RANDOM.nextBytes(buff);
//    [4,12,-1,45,56]  {-128 to 127}
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buff);

    }
}
