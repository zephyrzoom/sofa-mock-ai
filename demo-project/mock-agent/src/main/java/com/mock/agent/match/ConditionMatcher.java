package com.mock.agent.match;

import com.mock.agent.MockCase;

public class ConditionMatcher implements Matcher {

    @Override
    public boolean matches(MockCase mockCase, MatchContext context) {
        String condition = mockCase.getCondition();
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        return ConditionEvaluator.evaluate(condition, context.getRequestBody(), context.getRequestHeaders());
    }

    @Override
    public int priority() {
        return 70;
    }
}
