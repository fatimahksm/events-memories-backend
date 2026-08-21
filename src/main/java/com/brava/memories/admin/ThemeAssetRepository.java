package com.brava.memories.admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface ThemeAssetRepository extends JpaRepository<ThemeAsset, UUID> {
    List<ThemeAsset> findAllByOrderByCreatedAtDesc();
}
