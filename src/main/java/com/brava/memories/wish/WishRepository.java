package com.brava.memories.wish;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface WishRepository extends JpaRepository<Wish, UUID>{ List<Wish> findByEventIdOrderByCreatedAtDesc(UUID eventId); long countByEventId(UUID eventId); }
