package org.springframework.samples.petclinic.test.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TriageBudgetTest {

    @Test
    @DisplayName("The budget grants calls up to the limit and refuses the rest")
    void grantsUpToTheLimit() {
        TriageBudget budget = new TriageBudget(2);

        assertThat(budget.tryAcquire()).isTrue();
        assertThat(budget.tryAcquire()).isTrue();
        assertThat(budget.tryAcquire()).isFalse();
        assertThat(budget.tryAcquire()).isFalse();
    }

    @Test
    @DisplayName("A limit of zero or less grants nothing")
    void zeroLimitGrantsNothing() {
        assertThat(new TriageBudget(0).tryAcquire()).isFalse();
        assertThat(new TriageBudget(-3).tryAcquire()).isFalse();
    }
}
