package io.example.common.exception.grpc;

public class InsufficientBalanceException extends GrpcException {
    private final int currentBalance;
    private final int requestedAmount;

    public InsufficientBalanceException(int currentBalance, int requestedAmount) {
        super("INSUFFICIENT_BALANCE",
                String.format("Insufficient balance: current=%d, requested=%d",
                        currentBalance, requestedAmount));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    @Override
    public io.grpc.Status.Code getGrpcStatusCode() {
        return io.grpc.Status.Code.FAILED_PRECONDITION;
    }

    public int getCurrentBalance() {
        return currentBalance;
    }

    public int getRequestedAmount() {
        return requestedAmount;
    }
}