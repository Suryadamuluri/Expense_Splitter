package com.example.demo.model;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private double amount;

    @ManyToOne
    @JoinColumn(name = "paid_by")
    private User paidBy;

    @ManyToMany
    @JoinTable(
        name = "expense_shared_with",
        joinColumns = @JoinColumn(name = "expense_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> sharedWith = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    public Expense() {}

    public Expense(String description, double amount, User paidBy, Set<User> sharedWith, Group group) {
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.sharedWith = sharedWith;
        this.group = group;
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public User getPaidBy() { return paidBy; }
    public void setPaidBy(User paidBy) { this.paidBy = paidBy; }
    public Set<User> getSharedWith() { return sharedWith; }
    public void setSharedWith(Set<User> sharedWith) { this.sharedWith = sharedWith; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
}
