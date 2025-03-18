package org.example.orderService.dto;

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

    public static Builder builder() {
        return new Builder();
    }
    
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
    
    public static class Builder {
        private final PaymentRequest request = new PaymentRequest();
        
        public Builder orderId(String orderId) {
            request.setOrderId(orderId);
            return this;
        }
        
        public Builder amount(Double amount) {
            request.setAmount(amount);
            return this;
        }
        
        public Builder currency(String currency) {
            request.setCurrency(currency);
            return this;
        }
        
        public Builder paymentMethod(String paymentMethod) {
            request.setPaymentMethod(paymentMethod);
            return this;
        }
        
        public Builder cardNumber(String cardNumber) {
            request.setCardNumber(cardNumber);
            return this;
        }
        
        public Builder cardExpiryMonth(String cardExpiryMonth) {
            request.setCardExpiryMonth(cardExpiryMonth);
            return this;
        }
        
        public Builder cardExpiryYear(String cardExpiryYear) {
            request.setCardExpiryYear(cardExpiryYear);
            return this;
        }
        
        public Builder cardCvv(String cardCvv) {
            request.setCardCvv(cardCvv);
            return this;
        }
        
        public Builder cardHolderName(String cardHolderName) {
            request.setCardHolderName(cardHolderName);
            return this;
        }
        
        public PaymentRequest build() {
            return request;
        }
    }
}