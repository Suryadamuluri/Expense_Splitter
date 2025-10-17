package com.example.demo.model;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "app_user", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"firstName", "lastName", "email"})
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToMany(mappedBy = "members")
    private Set<Group> groups = new HashSet<>();

    public User() {
        this.lastName = "";
    }

    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName != null ? lastName :firstName;
        this.email = email;
    }

    public Long getId() { 
        return id; 
    }
    
    public String getFirstName() { 
        return firstName; 
    }
    
    public void setFirstName(String firstName) { 
        this.firstName = firstName; 
    }
    
    public String getLastName() { 
        return lastName; 
    }
    
    public void setLastName(String lastName) { 
        this.lastName = lastName != null ? lastName : ""; 
    }
    
    public String getFullName() {
        if (lastName == null || lastName.trim().isEmpty()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
    
    // Keep for backward compatibility
    public String getName() { 
        return getFullName(); 
    }
    
    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "";
        }
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    public Set<Group> getGroups() { 
        return groups; 
    }
    
    public void setGroups(Set<Group> groups) { 
        this.groups = groups; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && 
               Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}