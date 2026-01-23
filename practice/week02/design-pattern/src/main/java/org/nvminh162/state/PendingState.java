package org.nvminh162.state;

public class PendingState implements OrderState {
    @Override
    public void next(OrderContext context) {
        context.setState(new ProcessingState());
    }

    @Override
    public void prev(OrderContext context) {
        System.out.println("Order is in initial state");
    }

    @Override
    public void printStatus() {
        System.out.println("Order status: PENDING");
    }
}
