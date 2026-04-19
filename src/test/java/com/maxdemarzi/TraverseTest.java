package com.maxdemarzi;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TraverseTest {
    private Driver driver;
    private Neo4j embeddedDatabaseServer;

    @BeforeAll
    void initializeNeo4j() {
        this.embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withFixture(MODEL_STATEMENT)
                .withProcedure(DecisionTreeTraverser.class)
                .build();

        this.driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
    }

    @AfterAll
    void closeDriver(){
        this.driver.close();
        this.embeddedDatabaseServer.close();
    }

    @Test
    void barEntranceNo() {
        try (
                var driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
                var session = driver.session()
        ) {
            Record record = session.run("CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {gender:'male', age:'20'}) yield path return path").single();
            String answer = record.get(0).asPath().end().get("id").asString();
            assertThat(answer).isEqualTo("no");
        }
    }

    @Test
    void barEntranceYes() {
        try (
                var driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
                var session = driver.session()
        ) {
            Record record = session.run("CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {gender:'female', age:'19'}) yield path return path").single();
            String answer = record.get(0).asPath().end().get("id").asString();

            assertThat(answer).isEqualTo("yes");
        }
    }

    @Test
    void barEntranceAlsoYes() {
        try (
                var driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
                var session = driver.session()
        ) {
            Record record = session.run("CALL com.maxdemarzi.decision_tree.traverse('bar entrance', {gender:'male', age:'23'}) yield path return path").single();
            String answer = record.get(0).asPath().end().get("id").asString();

            assertThat(answer).isEqualTo("yes");
        }
    }

    @Test
    void funeralYeahYeahYeah() {
        try (
                var driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
                var session = driver.session()
        ) {
            Record record = session.run("CALL com.maxdemarzi.decision_tree.traverse('funeral', {answer_1:'yeah', answer_2:'yeah', answer_3:'yeah'}) yield path return path").single();
            String answer = record.get(0).asPath().end().get("id").asString();

            assertThat(answer).isEqualTo("incorrect");
        }
    }

    @Test
    void funeralWhatBlankBlank() {
        try (
                var driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
                var session = driver.session()
        ) {
            Record record = session.run("CALL com.maxdemarzi.decision_tree.traverse('funeral', {answer_1:'what', answer_2:'', answer_3:''}) yield path return path").single();
            String answer = record.get(0).asPath().end().get("id").asString();

            assertThat(answer).isEqualTo("unknown");
        }
    }

    @Test
    void funeralWhat() {
        try (
                var driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
                var session = driver.session()
        ) {
            Record record = session.run("CALL com.maxdemarzi.decision_tree.traverse('funeral', {answer_1:'what'}) yield path return path").single();
            String answer = record.get(0).asPath().end().get("prompt").asString();

            assertThat(answer).isEqualTo("What is the second answer?");
        }
    }

    @Test
    void funeralWhatYeahOkay() {
        try (
                var driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
                var session = driver.session()
        ) {
            Record record = session.run("CALL com.maxdemarzi.decision_tree.traverse('funeral', {answer_1:'what', answer_2:'yeah', answer_3:'okay'}) yield path return path").single();
            String answer = record.get(0).asPath().end().get("id").asString();

            assertThat(answer).isEqualTo("correct");
        }
    }
    private static final String MODEL_STATEMENT =
            "CREATE (tree:Tree { id: 'bar entrance' })" +
                    "CREATE (over21_rule:Rule { parameter_names: 'age', parameter_types:'int', expression:'age >= 21' })" +
                    "CREATE (gender_rule:Rule { parameter_names: 'age,gender', parameter_types:'int,String', expression:'(age >= 18) && gender.equals(\"female\")' })" +
                    "CREATE (answer_yes:Answer { id: 'yes'})" +
                    "CREATE (answer_no:Answer { id: 'no'})" +
                    "CREATE (tree)-[:HAS]->(over21_rule)" +
                    "CREATE (over21_rule)-[:IS_TRUE]->(answer_yes)" +
                    "CREATE (over21_rule)-[:IS_FALSE]->(gender_rule)" +
                    "CREATE (gender_rule)-[:IS_TRUE]->(answer_yes)" +
                    "CREATE (gender_rule)-[:IS_FALSE]->(answer_no)" +
                    "CREATE (age:Parameter {name:'age', type:'int', prompt:'How old are you?', expression:'(age > 0) &&  (age < 150)'})" +
                    "CREATE (gender:Parameter {name:'gender', type:'String', prompt:'What is your gender?', expression: '\"male\".equals(gender) || \"female\".equals(gender)'} )" +
                    "CREATE (over21_rule)-[:REQUIRES]->(age)" +
                    "CREATE (gender_rule)-[:REQUIRES]->(age)" +
                    "CREATE (gender_rule)-[:REQUIRES]->(gender)" +
                    "CREATE (tree2:Tree { id: 'funeral' })" +
                    "CREATE (good_man_rule:Rule { name: 'Was Lil Jon a good man?', parameter_names: 'answer_1', parameter_types:'String', script:'switch (answer_1) { case \"yeah\": return \"OPTION_1\"; case \"what\": return \"OPTION_2\"; case \"okay\": return \"OPTION_3\"; default: return \"UNKNOWN\"; }' })" +
                    "CREATE (good_man_two_rule:Rule { name: 'I said, was he a good man?', parameter_names: 'answer_2', parameter_types:'String', script:'switch (answer_2) { case \"yeah\": return \"OPTION_1\"; case \"what\": return \"OPTION_2\"; case \"okay\": return \"OPTION_3\"; default: return \"UNKNOWN\"; }' })" +
                    "CREATE (rest_in_peace_rule:Rule { name: 'May he rest in peace', parameter_names: 'answer_3', parameter_types:'String', script:'switch (answer_3) { case \"yeah\": return \"OPTION_1\"; case \"what\": return \"OPTION_2\"; case \"okay\": return \"OPTION_3\"; default: return \"UNKNOWN\"; } ' })" +
                    "CREATE (answer_correct:Answer { id: 'correct'})" +
                    "CREATE (answer_incorrect:Answer { id: 'incorrect'})" +
                    "CREATE (answer_unknown:Answer { id: 'unknown'})" +
                    "CREATE (tree2)-[:HAS]->(good_man_rule)" +
                    "CREATE (good_man_rule)-[:OPTION_1]->(answer_incorrect)" +
                    "CREATE (good_man_rule)-[:OPTION_2]->(good_man_two_rule)" +
                    "CREATE (good_man_rule)-[:OPTION_3]->(answer_incorrect)" +
                    "CREATE (good_man_rule)-[:UNKNOWN]->(answer_unknown)" +

                    "CREATE (good_man_two_rule)-[:OPTION_1]->(rest_in_peace_rule)" +
                    "CREATE (good_man_two_rule)-[:OPTION_2]->(answer_incorrect)" +
                    "CREATE (good_man_two_rule)-[:OPTION_3]->(answer_incorrect)" +
                    "CREATE (good_man_two_rule)-[:UNKNOWN]->(answer_unknown)" +

                    "CREATE (rest_in_peace_rule)-[:OPTION_1]->(answer_incorrect)" +
                    "CREATE (rest_in_peace_rule)-[:OPTION_2]->(answer_incorrect)" +
                    "CREATE (rest_in_peace_rule)-[:OPTION_3]->(answer_correct)" +
                    "CREATE (rest_in_peace_rule)-[:UNKNOWN]->(answer_unknown)" +

                    "CREATE (parameter1:Parameter {name:'answer_1', type:'String', prompt:'What is the first answer?', expression:'\"yeah\".equals(answer_1) || \"what\".equals(answer_1) || \"okay\".equals(answer_1) || \"\".equals(answer_1)'})" +
                    "CREATE (parameter2:Parameter {name:'answer_2', type:'String', prompt:'What is the second answer?', expression:'\"yeah\".equals(answer_2) || \"what\".equals(answer_2) || \"okay\".equals(answer_2) || \"\".equals(answer_2)'})" +
                    "CREATE (parameter3:Parameter {name:'answer_3', type:'String', prompt:'What is the third answer?', expression:'\"yeah\".equals(answer_3) || \"what\".equals(answer_3) || \"okay\".equals(answer_3) || \"\".equals(answer_3)'})" +
                    "CREATE (good_man_rule)-[:REQUIRES]->(parameter1)" +
                    "CREATE (good_man_two_rule)-[:REQUIRES]->(parameter2)" +
                    "CREATE (rest_in_peace_rule)-[:REQUIRES]->(parameter3)";
}
