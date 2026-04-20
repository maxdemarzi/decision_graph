package com.maxdemarzi;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.maxdemarzi.results.PathResult;
import com.maxdemarzi.results.ValidationResult;
import com.maxdemarzi.schema.Labels;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import static com.maxdemarzi.DecisionTree.isValid;

public class DecisionTreeTraverser {

    @Context
    public Transaction tx;

    @Context
    public Log log;

    @Procedure(name = "com.maxdemarzi.decision_tree.validate", mode = Mode.READ)
    @Description("CALL com.maxdemarzi.decision_tree.validate(parameter, value) - validate a parameter)")
    public Stream<ValidationResult> validateParameter(@Name("parameter") String name, @Name("value") String value) {
        boolean valid = false;
        Node parameter = tx.findNode(Labels.Parameter, "name", name);

        try {
            valid = isValid(parameter, value);
        } catch (Exception ignored) { }
        return Stream.of(new ValidationResult(parameter, valid));
    }

    @Procedure(name = "com.maxdemarzi.decision_tree.traverse", mode = Mode.READ)
    @Description("CALL com.maxdemarzi.decision_tree.traverse(tree, facts) - traverse stepwise decision tree")
    public Stream<PathResult> traverseStepWiseDecisionTree(@Name("tree") String id, @Name("facts") Map<String, String> facts) throws IOException {
        // Which Decision Tree are we interested in?
        Node tree = tx.findNode(Labels.Tree, "id", id);
        if ( tree != null) {
            // Find the paths by traversing this graph and the facts given
            return decisionPath(tree, facts);
        }
        return null;
    }

    private Stream<PathResult> decisionPath(Node tree, Map<String, String> facts) {
        TraversalDescription myTraversal = tx.traversalDescription()
                .depthFirst()
                .expand(new DecisionTreeExpander(facts))
                .evaluator(new DecisionTreeEvaluator(facts));

        Iterator<Path> paths = myTraversal.traverse(tree).iterator();

        Stream<Path> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(paths, Spliterator.ORDERED),
                false // set to true for a parallel stream
        );

        return stream.map(PathResult::new);
    }
}
