package com.voterapi.voter.controller;

import com.voterapi.voter.domain.CandidateVoterView;
import com.voterapi.voter.domain.Vote;
import com.voterapi.voter.domain.VoteCount;
import com.voterapi.voter.domain.VoteCountWinner;
import com.voterapi.voter.repository.CandidateRepository;
import com.voterapi.voter.repository.VoterRepository;
import com.voterapi.voter.service.CandidateService;
import com.voterapi.voter.service.VoterSeedDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RestController
public class VoterController {

    private MongoTemplate mongoTemplate;
    private VoterRepository voterRepository;
    private CandidateRepository candidateRepository;
    private VoterSeedDataService voterSeedDataService;
    private CandidateService candidateService;

    @Autowired
    public VoterController(MongoTemplate mongoTemplate,
                           VoterRepository voterRepository,
                           CandidateRepository candidateRepository,
                           VoterSeedDataService voterSeedDataService,
                           CandidateService candidateService) {
        this.mongoTemplate = mongoTemplate;
        this.candidateRepository = candidateRepository;
        this.voterRepository = voterRepository;
        this.voterSeedDataService = voterSeedDataService;
        this.candidateService = candidateService;
    }

    @RequestMapping(value = "/candidates/{election}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, List<CandidateVoterView>>> getCandidatesDb(@PathVariable("election") String election) {
        List<CandidateVoterView> results = candidateService.getCandidatesQueueDb(election);
        return new ResponseEntity<>(Collections.singletonMap("candidates", results), HttpStatus.OK);
    }

    @RequestMapping(value = "/drop/votes", method = RequestMethod.POST)
    public ResponseEntity<Void> deleteAllVotes() {

        voterRepository.deleteAll();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(value = "/drop/candidates", method = RequestMethod.POST)
    public ResponseEntity<Void> deleteAllCandidates() {

        candidateRepository.deleteAll();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping(value = "/results/{election}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, List<VoteCount>>> getResults(@PathVariable("election") String election) {

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria.where("election").is(election)),
                group("candidate").count().as("votes"),
                project("votes").and("candidate").previousOperation(),
                sort(Sort.Direction.DESC, "votes")
        );

        AggregationResults<VoteCount> groupResults
                = mongoTemplate.aggregate(aggregation, Vote.class, VoteCount.class);
        List<VoteCount> results = groupResults.getMappedResults();
        return new ResponseEntity<>(Collections.singletonMap("results", results), HttpStatus.OK);
    }

    @RequestMapping(value = "/results/{election}/votes", method = RequestMethod.GET)
    public ResponseEntity<VoteCountWinner> getTotalVotes(@PathVariable("election") String election) {

        Query query = new Query();
        query.addCriteria(Criteria.where("candidate").exists(true));
        query.addCriteria(Criteria.where("election").is(election));

        Long groupResults =
                mongoTemplate.count(query, Vote.class);
        VoteCountWinner result = new VoteCountWinner(groupResults.intValue());

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @RequestMapping(value = "/winners/{election}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, List<VoteCount>>> getWinners(@PathVariable("election") String election) {

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria.where("election").is(election)),
                group("candidate").count().as("votes"),
                match(Criteria.where("votes").is(getWinnersVotesInt(election))),
                project("votes").and("candidate").previousOperation(),
                sort(Sort.Direction.ASC, "candidate")
        );

        AggregationResults<VoteCount> groupResults
                = mongoTemplate.aggregate(aggregation, Vote.class, VoteCount.class);
        List<VoteCount> results = groupResults.getMappedResults();
        return new ResponseEntity<>(Collections.singletonMap("results", results), HttpStatus.OK);
    }

    @RequestMapping(value = "/winners/{election}/votes", method = RequestMethod.GET)
    public ResponseEntity<VoteCountWinner> getWinnersVotes(@PathVariable("election") String election) {

        VoteCountWinner result = new VoteCountWinner(getWinnersVotesInt(election));

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    private int getWinnersVotesInt(String election) {

        Aggregation aggregation = Aggregation.newAggregation(
                match(Criteria.where("election").is(election)),
                group("candidate").count().as("votes"),
                project("votes"),
                sort(Sort.Direction.DESC, "votes"),
                limit(1)
        );

        AggregationResults<VoteCountWinner> groupResults =
                mongoTemplate.aggregate(aggregation, Vote.class, VoteCountWinner.class);
        if (groupResults.getMappedResults().isEmpty()) {
            return 0;
        }

        return groupResults.getMappedResults().get(0).getVotes();
    }

    @RequestMapping(value = "/simulation/{election}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> getSimulationDb(@PathVariable("election") String election) {

        // voterRepository.deleteAll();
        voterSeedDataService.setRandomVotesDb(election);
        voterRepository.saveAll(voterSeedDataService.getVotes());
        Map<String, String> result = new HashMap<>();
        result.put("message", "Simulation data created using eventual consistency!");
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    // used by unit tests to create a known data set
    public void getSimulation(Map candidates, String election) {

        voterRepository.deleteAll();
        voterSeedDataService.votesFromMap(candidates, election);
        voterRepository.saveAll(voterSeedDataService.getVotes());
    }
}
