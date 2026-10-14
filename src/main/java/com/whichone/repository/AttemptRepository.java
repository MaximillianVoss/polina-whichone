package com.whichone.repository;

import com.whichone.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserUsernameOrderBySubmittedAtDesc(String username);
    Optional<QuizAttempt> findByIdAndUserUsername(long id, String username);
}
