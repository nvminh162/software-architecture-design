package org.nvminh162.state;

public class ProcessingState implements OrderState {
    @Override
    public void next(OrderContext context) {
        context.setState(new ShippedState());
    }

    @Override
    public void prev(OrderContext context) {
        context.setState(new PendingState());
    }

    @Override
    public void printStatus() {
        System.out.println("Order status: PROCESSING");
    }
}
