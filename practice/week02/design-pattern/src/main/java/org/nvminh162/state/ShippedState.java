package org.nvminh162.state;

public class ShippedState implements OrderState {
    @Override
    public void next(OrderContext context) {
        System.out.println("Order is already shipped");
    }

    @Override
    public void prev(OrderContext context) {
        context.setState(new ProcessingState());
    }

    @Override
    public void printStatus() {
        System.out.println("Order status: SHIPPED");
    }
}
