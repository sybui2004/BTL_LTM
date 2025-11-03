package com.example.memorygame.controller.friend;

import com.example.memorygame.model.user.FriendDTO;
import com.example.memorygame.model.user.FriendListDTO;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.FriendApi;
import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.utils.UserApi;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Manages friend list, tabs, and search functionality
 */
public class FriendListManager {
    private final VBox listContainer;
    private final HBox searchContainer;
    private final TextField txtSearch;
    private final ToggleButton tabFriends;
    private final ToggleButton tabStrangers;
    private final ToggleButton tabRecent;
    private final ToggleGroup tabsGroup = new ToggleGroup();
    private final FriendItemBuilder itemBuilder;
    
    private String lastSearchQuery = null;
    private Tab currentTab = Tab.FRIENDS;
    
    public enum Tab { FRIENDS, STRANGERS, RECENT }
    
    public FriendListManager(VBox listContainer, HBox searchContainer, TextField txtSearch,
                            ToggleButton tabFriends, ToggleButton tabStrangers, ToggleButton tabRecent,
                            FriendItemBuilder itemBuilder) {
        this.listContainer = listContainer;
        this.searchContainer = searchContainer;
        this.txtSearch = txtSearch;
        this.tabFriends = tabFriends;
        this.tabStrangers = tabStrangers;
        this.tabRecent = tabRecent;
        this.itemBuilder = itemBuilder;
    }
    
    public void setupTabs() {
        tabFriends.setToggleGroup(tabsGroup);
        tabStrangers.setToggleGroup(tabsGroup);
        tabRecent.setToggleGroup(tabsGroup);
        tabsGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) return;
            // Play button sound when switching tabs
            SoundManager.playSound("button.wav");
            if (newT == tabFriends) switchTab(Tab.FRIENDS);
            else if (newT == tabStrangers) switchTab(Tab.STRANGERS);
            else switchTab(Tab.RECENT);
        });
        tabFriends.setSelected(true);
    }
    
    public void switchTab(Tab tab) {
        currentTab = tab;
        
        if (tab == Tab.STRANGERS) {
            setSearchVisible(true);
            listContainer.getChildren().clear();
            lastSearchQuery = null;
            if (txtSearch != null) {
                txtSearch.clear();
            }
            return;
        }
        
        // Clear search query and field when leaving Strangers tab
        lastSearchQuery = null;
        if (txtSearch != null) {
            txtSearch.clear();
        }
        setSearchVisible(false);
        
        new Thread(() -> {
            UserSummary currentUser = UserApi.getCurrentUser();
            Long currentUserId = (currentUser != null) ? currentUser.id : null;
            
            if (tab == Tab.FRIENDS) {
                FriendListDTO friendList = FriendApi.getFriendList();
                if (friendList != null && friendList.friends != null) {
                    Platform.runLater(() -> populateFriendList(friendList.friends));
                } else {
                    Platform.runLater(() -> listContainer.getChildren().clear());
                }
            } else if (tab == Tab.RECENT) {
                List<UserSummary> users = UserApi.getRecentPlayers();
                Platform.runLater(() -> populateList(users, currentUserId));
            } else {
                List<UserSummary> users = UserApi.getAllUsers();
                Platform.runLater(() -> populateList(users, currentUserId));
            }
        }).start();
    }
    
    public void refreshCurrentTab() {
        if (tabFriends.isSelected()) {
            switchTab(Tab.FRIENDS);
        } else if (tabStrangers.isSelected()) {
            if (lastSearchQuery != null && !lastSearchQuery.isEmpty()) {
                executeSearch(lastSearchQuery);
            }
        } else if (tabRecent.isSelected()) {
            switchTab(Tab.RECENT);
        }
    }
    
    public void handleSearch() {
        String text = txtSearch.getText();
        if (text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;
        
        lastSearchQuery = trimmed;
        executeSearch(trimmed);
    }
    
    private void executeSearch(String query) {
        long id;
        try {
            id = Long.parseLong(query);
        } catch (NumberFormatException ex) {
            listContainer.getChildren().setAll(new Label("Invalid id"));
            lastSearchQuery = null;
            return;
        }
        
        new Thread(() -> {
            UserSummary currentUser = UserApi.getCurrentUser();
            Long currentUserId = (currentUser != null) ? currentUser.id : null;
            
            UserSummary user = UserApi.getUserById(id);
            Platform.runLater(() -> {
                if (user == null) {
                    listContainer.getChildren().setAll(new Label("No user found"));
                    lastSearchQuery = null;
                } else if (currentUserId != null && user.id == currentUserId) {
                    listContainer.getChildren().setAll(new Label("Cannot search for yourself"));
                    lastSearchQuery = null;
                } else {
                    populateList(List.of(user));
                }
            });
        }).start();
    }
    
    private void setSearchVisible(boolean visible) {
        searchContainer.setVisible(visible);
        searchContainer.setManaged(visible);
    }
    
    private void populateList(List<UserSummary> users) {
        populateList(users, null);
    }
    
    private void populateList(List<UserSummary> users, Long excludeUserId) {
        listContainer.getChildren().clear();
        int displayIndex = 0;
        for (UserSummary u : users) {
            if (excludeUserId != null && u.id == excludeUserId) {
                continue;
            }
            listContainer.getChildren().add(itemBuilder.createFriendItem(u, displayIndex));
            displayIndex++;
        }
    }
    
    private void populateFriendList(List<FriendDTO> friends) {
        listContainer.getChildren().clear();
        for (int i = 0; i < friends.size(); i++) {
            FriendDTO friend = friends.get(i);
            listContainer.getChildren().add(itemBuilder.createFriendItemFromDTO(friend, i));
        }
    }
    
    public Tab getCurrentTab() {
        return currentTab;
    }
}

