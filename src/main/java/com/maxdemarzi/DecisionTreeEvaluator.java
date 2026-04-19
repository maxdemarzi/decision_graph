package com.maxdemarzi;

import com.maxdemarzi.schema.Labels;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;

import java.util.Map;

public class DecisionTreeEvaluator implements PathEvaluator {
    private Map<String, String> facts;

    DecisionTreeEvaluator(Map<String, String> facts) {
        this.facts = facts;
    }

    @Override
    public Evaluation evaluate(Path path, BranchState branchState) {
        Node last = path.endNode();

        // If we get to an Answer, then stop traversing because we found a valid path.
        if (last.hasLabel(Labels.Answer)) {
            return Evaluation.INCLUDE_AND_PRUNE;
        }

        if(last.hasLabel(Labels.Parameter)) {
            // If we get to a Parameter, check if we have a fact for it.
            if (facts.containsKey(last.getProperty("name", "").toString())) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            return Evaluation.INCLUDE_AND_PRUNE;
        }

        // If not, continue down this path if there is anything else to find.
        return Evaluation.EXCLUDE_AND_CONTINUE;
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }
}