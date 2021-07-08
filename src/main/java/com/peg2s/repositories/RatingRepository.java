package com.peg2s.repositories;

import com.peg2s.models.PersonalRating;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RatingRepository extends CrudRepository<PersonalRating, Long> {
    List<PersonalRating> findByUser_login(String login);

    boolean existsByBook_IdAndUser_Id(Long bookId, Long userId);

    PersonalRating findByBook_IdAndUser_Id(Long bookId, Long userId);
}
