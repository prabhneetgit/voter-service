package com.voterapi.voter.repository;

import com.voterapi.voter.domain.Vote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VoterRepository extends MongoRepository<Vote, String> {

}
