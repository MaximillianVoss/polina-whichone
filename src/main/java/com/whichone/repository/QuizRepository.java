package com.whichone.repository;

import com.whichone.domain.Quiz;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByPublishedTrueOrderByPublishedAtDesc();
    List<Quiz> findByAuthorUsernameOrderByCreatedAtDesc(String username);

    /** Draft changes and publication take the same lock to prevent race-based edits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.id = :id")
    Optional<Quiz> findLockedById(@Param("id") long id);
}
