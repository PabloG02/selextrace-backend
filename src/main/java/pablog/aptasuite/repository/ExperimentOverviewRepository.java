package pablog.aptasuite.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pablog.aptasuite.model.ExperimentOverviewDocument;

@Repository
public interface ExperimentOverviewRepository extends MongoRepository<ExperimentOverviewDocument, String> {
}
