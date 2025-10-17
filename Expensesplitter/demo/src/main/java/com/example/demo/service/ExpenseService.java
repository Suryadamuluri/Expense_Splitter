package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.model.Expense;
import com.example.demo.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public Expense saveExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public List<Expense> getExpensesByGroup(Long groupId) {
        return expenseRepository.findAll().stream()
                .filter(e -> e.getGroup() != null && e.getGroup().getId().equals(groupId))
                .toList();
    }

    public Map<String, Double> calculateBalances(List<Expense> expenses) {
        Map<String, BigDecimal> balances = new HashMap<>();
        BigDecimal centThreshold = new BigDecimal("0.01");

        if (expenses == null) return Collections.emptyMap();

        for (Expense expense : expenses) {
            if (expense == null) continue;

            Double amount = expense.getAmount();
            BigDecimal totalAmount = BigDecimal.ZERO;
            if (amount != null) {
                totalAmount = new BigDecimal(String.format("%.2f", amount));
            }

            if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            User paidBy = expense.getPaidBy();
            String payer = paidBy != null ? paidBy.getFullName() : null; // Use full name

            Set<User> sharedWith = expense.getSharedWith();

            if (sharedWith == null || sharedWith.isEmpty()) {
                if (expense.getGroup() != null && expense.getGroup().getMembers() != null) {
                    sharedWith = expense.getGroup().getMembers();
                }
            }

            if (sharedWith == null || sharedWith.isEmpty()) continue;

            int numPeople = sharedWith.size();
            BigDecimal sharePerPerson = totalAmount.divide(new BigDecimal(numPeople), 2, RoundingMode.HALF_EVEN);

            if (payer != null) {
                balances.put(payer, balances.getOrDefault(payer, BigDecimal.ZERO).add(totalAmount));
            }

            for (User user : sharedWith) {
                if (user == null) continue;
                String userName = user.getFullName(); // Use full name
                balances.put(userName, balances.getOrDefault(userName, BigDecimal.ZERO).subtract(sharePerPerson));
            }
        }

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, BigDecimal> e : balances.entrySet()) {
            BigDecimal v = e.getValue() == null ? BigDecimal.ZERO : e.getValue();
            if (v.abs().compareTo(centThreshold) <= 0) v = BigDecimal.ZERO;
            v = v.setScale(2, RoundingMode.HALF_EVEN);
            result.put(e.getKey(), v.doubleValue());
        }

        return result;
    }

    public List<String> calculateSettlements(Map<String, Double> balances) {
        List<String> settlements = new ArrayList<>();
        BigDecimal centThreshold = new BigDecimal("0.01");

        if (balances == null || balances.isEmpty()) return settlements;

        List<Map.Entry<String, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<String, BigDecimal>> debtors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            BigDecimal val = entry.getValue() == null ? BigDecimal.ZERO : BigDecimal.valueOf(entry.getValue());
            if (val.compareTo(centThreshold) > 0) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), val));
            } else if (val.compareTo(centThreshold.negate()) < 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), val.abs()));
            }
        }

        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        debtors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int i = 0, j = 0;
        while (i < creditors.size() && j < debtors.size()) {
            String creditor = creditors.get(i).getKey();
            String debtor = debtors.get(j).getKey();
            BigDecimal credit = creditors.get(i).getValue();
            BigDecimal debt = debtors.get(j).getValue();

            BigDecimal settlement = credit.min(debt).setScale(2, RoundingMode.HALF_EVEN);

            if (settlement.compareTo(centThreshold) > 0) {
                settlements.add(String.format("%s pays %s $%.2f", debtor, creditor, settlement.doubleValue()));
            }

            creditors.get(i).setValue(credit.subtract(settlement));
            debtors.get(j).setValue(debt.subtract(settlement));

            if (creditors.get(i).getValue().compareTo(centThreshold) <= 0) i++;
            if (debtors.get(j).getValue().compareTo(centThreshold) <= 0) j++;
        }

        return settlements;
    }
}