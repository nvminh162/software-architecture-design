package org.nvminh162.state;

public class OrderContext {
    private OrderState state;

    public OrderContext() {
        state = new PendingState();
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public void nextState() {
        state.next(this);
    }

    public void prevState() {
        state.prev(this);
    }

    public void printStatus() {
        state.printStatus();
    }
}
