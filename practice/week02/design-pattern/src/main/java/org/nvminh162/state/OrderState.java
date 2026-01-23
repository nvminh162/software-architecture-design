package org.nvminh162.state;

public interface OrderState {
    void next(OrderContext context);
    void prev(OrderContext context);
    void printStatus();
}
