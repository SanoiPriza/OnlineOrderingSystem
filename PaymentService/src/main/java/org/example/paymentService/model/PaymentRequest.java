package org.example.paymentService.model;

public class PaymentRequest {
    private String orderId;
    private Double amount;
    private String currency;
    private String paymentMethod;
    private String cardNumber;
    private String cardExpiryMonth;
    private String cardExpiryYear;
    private String cardCvv;
    private String cardHolderName;

    public PaymentRequest() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    
    public String getCardExpiryMonth() { return cardExpiryMonth; }
    public void setCardExpiryMonth(String cardExpiryMonth) { this.cardExpiryMonth = cardExpiryMonth; }
    
    public String getCardExpiryYear() { return cardExpiryYear; }
    public void setCardExpiryYear(String cardExpiryYear) { this.cardExpiryYear = cardExpiryYear; }
    
    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }
    
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
}