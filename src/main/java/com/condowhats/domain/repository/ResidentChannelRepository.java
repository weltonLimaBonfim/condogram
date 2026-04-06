package com.condowhats.domain.repository;

import com.condowhats.domain.model.ResidentChannel;
import com.condowhats.domain.port.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResidentChannelRepository extends JpaRepository<ResidentChannel, Long> {
    Optional<ResidentChannel> findByExternalIdAndChannel(String externalId, Channel channel);

    Optional<ResidentChannel> findByResidentIdAndChannel(Long residentId, Channel channel);

    List<ResidentChannel> findByResidentCondominium_IdAndChannelAndOptedInTrue(Long condoId, Channel channel);
}
