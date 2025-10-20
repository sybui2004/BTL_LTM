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

    List<User> findAllByStatus(UserStatus status);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query(value = """
        SELECT 
            u.id AS id,
            u.display_name AS displayName,
            u.avatar_url AS avatarUrl,
            COALESCE(SUM(
                CASE 
                    WHEN m.player1_id = u.id THEN m.player1_score
                    WHEN m.player2_id = u.id THEN m.player2_score
                    ELSE 0
                END
            ), 0) AS totalScore,
            COALESCE(SUM(
                CASE 
                    WHEN (m.player1_score > m.player2_score AND m.player1_id = u.id) 
                      OR (m.player2_score > m.player1_score AND m.player2_id = u.id)
                    THEN 1 ELSE 0
                END
            ), 0) AS winCount
        FROM users u
        LEFT JOIN game_match m 
               ON m.player1_id = u.id OR m.player2_id = u.id
        GROUP BY u.id, u.display_name, u.avatar_url
        ORDER BY totalScore DESC, winCount DESC
        """, nativeQuery = true)
    List<UserRankingProjection> getUserRankingNative();
}
