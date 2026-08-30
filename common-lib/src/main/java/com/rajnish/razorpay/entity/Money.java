package com.rajnish.razorpay.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Money {
    private int amountUnits;
    private String currency;


    public static Money of(int amountUnits, String currency) {
        return new Money(amountUnits, currency);
    }

    private static Money inr(int amountUnits){
        return new Money(amountUnits, "INR");
    }

    public Money addd(Money money) {
        if (!this.currency.equals(money.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(this.amountUnits + money.amountUnits, this.currency);
    }

    public Money sub(Money money) {
        if (!this.currency.equals(money.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(this.amountUnits - money.amountUnits, this.currency);
    }
}
