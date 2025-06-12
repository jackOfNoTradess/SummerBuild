package com.example.SummerBuild.service;

import com.example.SummerBuild.dto.ParticipatesDto;
import com.example.SummerBuild.mapper.ParticipatesMapper;
import com.example.SummerBuild.model.Participates;
import com.example.SummerBuild.repository.ParticipatesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipatesService {
    
    private final ParticipatesRepository participatesRepository;
    private final ParticipatesMapper participatesMapper;
    
    /**
     * Add a user to an event (create participation)
     */
    public ParticipatesDto addParticipation(UUID userId, UUID eventId) {
        // Check if user is already participating in the event
        if (participatesRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new IllegalArgumentException("User is already participating in this event");
        }
        
        Participates participation = Participates.builder()
                .userId(userId)
                .eventId(eventId)
                .build();
        
        Participates savedParticipation = participatesRepository.save(participation);
        // TODO : Atomic Lock on capacity, don't subtract capacity when there is none.
        return participatesMapper.toDto(savedParticipation);
    }
    
    /**
     * Remove a user from an event (delete participation)
     */
    public void removeParticipation(UUID userId, UUID eventId) {
        Optional<Participates> participation = participatesRepository.findByUserIdAndEventId(userId, eventId);
        if (participation.isPresent()) {
            participatesRepository.delete(participation.get());
            // TODO : Atomic Lock on capacity, don't add capacity when already full.
        } else {
            throw new IllegalArgumentException("User is not participating in this event");
        }
    }
    
    /**
     * Get all events a user is participating in
     */
    @Transactional(readOnly = true)
    public List<ParticipatesDto> getUserParticipations(UUID userId) {
        List<Participates> participations = participatesRepository.findByUserId(userId);
        return participatesMapper.toDtoList(participations);
    }
    
    /**
     * Get all participants for an event
     */
    @Transactional(readOnly = true)
    public List<ParticipatesDto> getEventParticipants(UUID eventId) {
        List<Participates> participants = participatesRepository.findByEventId(eventId);
        return participatesMapper.toDtoList(participants);
    }
    
    /**
     * Check if a user is participating in an event
     */
    @Transactional(readOnly = true)
    public boolean isUserParticipating(UUID userId, UUID eventId) {
        return participatesRepository.existsByUserIdAndEventId(userId, eventId);
    }
    
    /**
     * Get the number of participants for an event
     */
    @Transactional(readOnly = true)
    public long getParticipantCount(UUID eventId) {
        return participatesRepository.countParticipantsByEventId(eventId);
    }
    
    /**
     * Get the number of events a user is participating in
     */
    @Transactional(readOnly = true)
    public long getUserEventCount(UUID userId) {
        return participatesRepository.countEventsByUserId(userId);
    }
    
    /**
     * Get all participations
     */
    @Transactional(readOnly = true)
    public List<ParticipatesDto> getAllParticipations() {
        List<Participates> participations = participatesRepository.findAll();
        return participatesMapper.toDtoList(participations);
    }
    
    /**
     * Get participation by ID
     */
    @Transactional(readOnly = true)
    public Optional<ParticipatesDto> getParticipationById(UUID id) {
        return participatesRepository.findById(id)
                .map(participatesMapper::toDto);
    }
}
