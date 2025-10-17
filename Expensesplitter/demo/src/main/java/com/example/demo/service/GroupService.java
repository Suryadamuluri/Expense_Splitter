package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.repository.GroupRepository;
import com.example.demo.model.Group;
import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    public Group saveGroup(Group group) {
        return groupRepository.save(group);
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group findById(Long id) {
        return groupRepository.findById(id).orElseThrow(() -> 
            new RuntimeException("Group not found with id: " + id));
    }

    public boolean existsByName(String name) {
        return groupRepository.existsByNameIgnoreCase(name);
    }

    public Optional<Group> findByName(String name) {
        return groupRepository.findByNameIgnoreCase(name);
    }
}