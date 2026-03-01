package com.bitmask.leaderboard.domain.ranking.formula;

import com.bitmask.leaderboard.error.exception.InvalidFormulaException;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@AllArgsConstructor
public final class RankingFormula {
    private final String source;
    private final Set<String> allowedVars;
    private final Expression compiled;

    public RankingFormula(
            @Nonnull String source,
            @Nonnull Set<String> allowedVars
    ) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.allowedVars = Set.copyOf(
                Objects.requireNonNull(allowedVars, "allowedVars cannot be null")
        );
        compiled = compile(source);
    }

    private Expression compile(String source) {
        try {
            return new SpelExpressionParser().parseExpression(source);
        } catch (ParseException e) {
            throw new InvalidFormulaException("Failed to parse formula syntax: " + e.getMessage(), e);
        }
    }

    public double evaluate(Map<String, Double> vars) {
        if (!allowedVars.containsAll(vars.keySet())) {
            throw new IllegalArgumentException("Provided variables contain unauthorized keys");
        }

        StandardEvaluationContext ctx = new StandardEvaluationContext();

        vars.forEach(ctx::setVariable);

        try {
            Double result = compiled.getValue(ctx, Double.class);
            if (result == null) {
                throw new InvalidFormulaException("Formula evaluated to null instead of a number");
            }

            if (Double.isNaN(result) || Double.isInfinite(result)) {
                throw new InvalidFormulaException("Formula evaluated to an invalid number (NaN or Infinity)");
            }
            return result;

        } catch (SpelEvaluationException e) {
            throw new InvalidFormulaException("Error evaluating formula with given values: " + e.getMessage(), e);
        }
    }

    public String source() {
        return source;
    }
}
