package net.guilhermegomes.backend.repository;

import net.guilhermegomes.backend.domain.userQuery.UserQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQueryRepository extends JpaRepository<UserQuery, Long> {
}
