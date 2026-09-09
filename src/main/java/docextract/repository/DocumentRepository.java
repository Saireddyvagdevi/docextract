package docextract.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import docextract.model.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}