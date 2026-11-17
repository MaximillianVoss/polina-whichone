package com.whichone.repository;

import com.whichone.domain.Quiz;
import com.whichone.domain.QuizType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByPublishedTrueAndArchivedFalseOrderByPublishedAtDesc();
    List<Quiz> findByAuthorUsernameAndArchivedFalseOrderByCreatedAtDesc(String username);
    @Query("""
            select count(q) > 0 from Quiz q
            where q.author.id = :authorId and q.type = :type and q.published = true and q.archived = false
            and (q.demo = true or (q.title = :title and q.description = :description))
            """)
    boolean hasActiveDemo(@Param("authorId") long authorId, @Param("type") QuizType type,
                          @Param("title") String title, @Param("description") String description);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.author.id = :authorId and q.published = true and q.archived = false order by q.id")
    List<Quiz> findActivePublishedForUpdate(@Param("authorId") long authorId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select q from Quiz q where q.id = :id")
    Optional<Quiz> findForAttempt(@Param("id") long id);

    /** Draft changes and publication take the same lock to prevent race-based edits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.id = :id")
    Optional<Quiz> findLockedById(@Param("id") long id);
}
