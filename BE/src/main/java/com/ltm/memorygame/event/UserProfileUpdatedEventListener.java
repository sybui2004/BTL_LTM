package com.ltm.memorygame.event;

import com.ltm.memorygame.dao.user.FriendRepository;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.model.enums.FriendStatus;
import com.ltm.memorygame.model.user.Friend;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.tcp.TCPServer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserProfileUpdatedEventListener {
    
    private final TCPServer tcpServer;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    
    @EventListener
    @Transactional(readOnly = true)
    public void handleUserProfileUpdated(UserProfileUpdatedEvent event) {
        try {
            // Get the user who updated their profile
            User updatedUser = userRepository.findById(event.getUserId())
                    .orElse(null);
            
            if (updatedUser == null) {
                return;
            }
            
            // Get all accepted friends of this user
            List<Friend> friendRelations = friendRepository.findActiveByUser(updatedUser);
            
            // Extract usernames of all friends (only accepted friends)
            Set<String> friendUsernames = new HashSet<>();
            for (Friend fr : friendRelations) {
                if (fr.getStatus() == FriendStatus.ACCEPTED) {
                    User friendUser = fr.getSender().getId().equals(event.getUserId())
                            ? fr.getReceiver()
                            : fr.getSender();
                    friendUsernames.add(friendUser.getUsername());
                }
            }
            
            // Send TCP notification to all online friends
            if (!friendUsernames.isEmpty()) {
                tcpServer.broadcastProfileUpdatedToFriends(event.getUsername(), friendUsernames);
            }
        } catch (Exception e) {
            System.err.println("[UserProfileUpdatedEventListener] Error handling profile update: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
