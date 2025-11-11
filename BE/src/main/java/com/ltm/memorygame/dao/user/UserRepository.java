package com.ltm.memorygame.dao.user;

import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository  extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    @Query("""
    SELECT u
    FROM User u
    WHERE LOWER(u.displayName) LIKE LOWER(CONCAT(:prefix, '%'))
       OR STR(u.id) LIKE CONCAT(:prefix, '%')
    """)
    List<User> searchByPrefix(@org.springframework.data.repository.query.Param("prefix") String prefix);

    @Query("""
    SELECT u
    FROM User u
    WHERE (LOWER(u.displayName) LIKE LOWER(CONCAT(:prefix, '%'))
        OR STR(u.id) LIKE CONCAT(:prefix, '%'))
      AND u.id <> :excludeId
    """)
    List<User> searchByPrefixExcluding(@org.springframework.data.repository.query.Param("prefix") String prefix,
                                       @org.springframework.data.repository.query.Param("excludeId") Long excludeId);

    List<User> findAllByStatus(UserStatus status);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query(value = """
        SELECT 
            u.id AS id,
            u.display_name AS displayName,
            u.avatar_url AS avatarUrl,
            COALESCE(u.score, 0) AS totalScore,
            COALESCE(SUM(
                CASE 
                    -- Only count finished matches (has end_time and status != 'PLAYING')
                    WHEN m.end_time IS NOT NULL 
                      AND m.status != 'PLAYING'
                      AND (
                        -- Player1 wins if player1_rank_points_change > 0
                        (m.player1_rank_points_change IS NOT NULL AND m.player1_rank_points_change > 0 AND m.player1_id = u.id)
                        OR
                        -- Player2 wins if player2_rank_points_change > 0
                        (m.player2_rank_points_change IS NOT NULL AND m.player2_rank_points_change > 0 AND m.player2_id = u.id)
                      )
                    THEN 1 ELSE 0
                END
            ), 0) AS winCount
        FROM users u
        LEFT JOIN game_match m 
               ON (m.player1_id = u.id OR m.player2_id = u.id)
               AND m.end_time IS NOT NULL
               AND m.status != 'PLAYING'
        GROUP BY u.id, u.display_name, u.avatar_url, u.score
        ORDER BY u.score DESC, winCount DESC
        """, nativeQuery = true)
    List<UserRankingProjection> getUserRankingNative();
}
