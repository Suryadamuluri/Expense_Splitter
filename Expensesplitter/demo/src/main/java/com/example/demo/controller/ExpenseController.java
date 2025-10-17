package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.demo.service.ExpenseService;
import com.example.demo.service.GroupService;
import com.example.demo.service.UserService;
import com.example.demo.model.*;

import java.util.*;

@Controller
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("groups", groupService.getAllGroups());
        model.addAttribute("users", userService.getAllUsers());
        return "index";
    }

    @PostMapping("/groups/add")
    public String addGroup(@RequestParam String name, 
                          @RequestParam(required = false) String memberNames,
                          RedirectAttributes redirectAttributes) {
        
        // Check if group name already exists
        if (groupService.existsByName(name.trim())) {
            redirectAttributes.addFlashAttribute("error", "Group name '" + name + "' already exists! Please use a different name.");
            return "redirect:/";
        }

        Group group = new Group(name.trim());

        if (memberNames != null && !memberNames.trim().isEmpty()) {
            String[] names = memberNames.split(",");
            Set<User> members = new HashSet<>();
            for (String memberName : names) {
                String trimmedName = memberName.trim();
                if (!trimmedName.isEmpty()) {
                    // Support "FirstName LastName" format
                    String[] nameParts = trimmedName.split("\\s+");
                    String firstName = nameParts[0];
                    String lastName = nameParts.length > 1 ? nameParts[1] : "";
                    
                    User user = userService.findOrCreateUser(firstName, lastName, 
                                                            firstName.toLowerCase() + 
                                                            (lastName.isEmpty() ? "" : "." + lastName.toLowerCase()) + 
                                                            "@example.com");
                    members.add(user);
                }
            }
            group.setMembers(members);
        }

        groupService.saveGroup(group);
        redirectAttributes.addFlashAttribute("success", "Group '" + name + "' created successfully!");
        return "redirect:/";
    }

    @PostMapping("/groups/{groupId}/addMember")
    public String addMemberToGroup(@PathVariable Long groupId,
                                   @RequestParam String memberName,
                                   RedirectAttributes redirectAttributes) {
        try {
            Group group = groupService.findById(groupId);
            
            String trimmedName = memberName.trim();
            if (!trimmedName.isEmpty()) {
                String[] nameParts = trimmedName.split("\\s+");
                String firstName = nameParts[0];
                String lastName = nameParts.length > 1 ? nameParts[1] : "";
                
                User user = userService.findOrCreateUser(firstName, lastName,
                                                        firstName.toLowerCase() + 
                                                        (lastName.isEmpty() ? "" : "." + lastName.toLowerCase()) + 
                                                        "@example.com");
                
                // Check if user already in group
                if (group.getMembers().contains(user)) {
                    redirectAttributes.addFlashAttribute("error", "User '" + user.getFullName() + "' is already in this group!");
                } else {
                    group.getMembers().add(user);
                    groupService.saveGroup(group);
                    redirectAttributes.addFlashAttribute("success", "Member '" + user.getFullName() + "' added successfully!");
                }
            }
            
            return "redirect:/groups/" + groupId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding member: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/groups/{groupId}")
    public String viewGroup(@PathVariable Long groupId, Model model) {
        Group group = groupService.findById(groupId);
        model.addAttribute("group", group);
        return "group";
    }

    @GetMapping("/expenses")
    public String expenses(@RequestParam(required = false) Long groupId, Model model) {
        List<Expense> expenses;
        List<User> usersToShow;
        
        if (groupId != null) {
            expenses = expenseService.getExpensesByGroup(groupId);
            Group selectedGroup = groupService.findById(groupId);
            usersToShow = new ArrayList<>(selectedGroup.getMembers());
            model.addAttribute("selectedGroupId", groupId);
        } else {
            expenses = expenseService.getAllExpenses();
            usersToShow = userService.getAllUsers();
        }

        model.addAttribute("expenses", expenses);
        model.addAttribute("groups", groupService.getAllGroups());
        model.addAttribute("users", usersToShow);
        return "expense";
    }

    @PostMapping("/expenses/add")
    public String addExpense(
            @RequestParam String description,
            @RequestParam double amount,
            @RequestParam Long paidById,
            @RequestParam Long groupId,
            @RequestParam(required = false) List<Long> sharedWithIds,
            RedirectAttributes redirectAttributes) {

        try {
            User paidBy = userService.findById(paidById);
            Group group = groupService.findById(groupId);

            Set<User> sharedWith = new HashSet<>();
            if (sharedWithIds != null && !sharedWithIds.isEmpty()) {
                for (Long userId : sharedWithIds) {
                    User u = userService.findById(userId);
                    if (u != null) sharedWith.add(u);
                }
            } else {
                if (group != null && group.getMembers() != null) {
                    sharedWith.addAll(group.getMembers());
                }
            }

            Expense expense = new Expense();
            expense.setDescription(description);
            expense.setAmount(amount);
            expense.setPaidBy(paidBy);
            expense.setSharedWith(sharedWith);
            expense.setGroup(group);

            expenseService.saveExpense(expense);
            redirectAttributes.addFlashAttribute("success", "Expense added successfully!");

            return "redirect:/expenses?groupId=" + groupId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding expense: " + e.getMessage());
            return "redirect:/expenses" + (groupId != null ? "?groupId=" + groupId : "");
        }
    }

    @GetMapping("/groups/{groupId}/members")
    @ResponseBody
    public List<Map<String, Object>> getGroupMembers(@PathVariable Long groupId) {
        Group group = groupService.findById(groupId);
        if (group == null) return Collections.emptyList();

        List<Map<String, Object>> members = new ArrayList<>();
        if (group.getMembers() != null) {
            for (User u : group.getMembers()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("name", u.getFullName()); // Use full name
                members.add(m);
            }
        }
        return members;
    }

    @GetMapping("/summary")
    public String summary(@RequestParam(required = false) Long groupId, Model model) {
        List<Expense> expenses;
        
        if (groupId != null) {
            expenses = expenseService.getExpensesByGroup(groupId);
            model.addAttribute("selectedGroupId", groupId);
            
            Group selectedGroup = groupService.findById(groupId);
            Map<String, Double> balances = expenseService.calculateBalances(expenses);
            
            Map<String, Double> filteredBalances = new HashMap<>();
            if (selectedGroup != null && selectedGroup.getMembers() != null) {
                for (User member : selectedGroup.getMembers()) {
                    String memberFullName = member.getFullName();
                    filteredBalances.put(memberFullName, balances.getOrDefault(memberFullName, 0.0));
                }
            }
            
            List<String> settlements = expenseService.calculateSettlements(filteredBalances);
            model.addAttribute("balances", filteredBalances);
            model.addAttribute("settlements", settlements);
        } else {
            expenses = expenseService.getAllExpenses();
            Map<String, Double> balances = expenseService.calculateBalances(expenses);
            List<String> settlements = expenseService.calculateSettlements(balances);
            model.addAttribute("balances", balances);
            model.addAttribute("settlements", settlements);
        }

        model.addAttribute("expenses", expenses);
        model.addAttribute("groups", groupService.getAllGroups());

        return "summary";
    }
}