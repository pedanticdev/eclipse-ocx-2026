package com.azul.eclipseocx2026.data;

import com.azul.eclipseocx2026.model.ConferenceTalk;
import com.azul.eclipseocx2026.model.Speaker;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@ApplicationScoped
public class DataLoader {

    private static final Logger LOG = Logger.getLogger(DataLoader.class.getName());

    @PersistenceContext
    private EntityManager em;

    @Inject
    private EmbeddingModel embeddingModel;

    @Inject
    private EmbeddingStore<TextSegment> embeddingStore;

    @Transactional
    public void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LOG.info("Starting data ingestion...");

        List<Document> documents = new ArrayList<>();

        documents.addAll(loadConferenceTalks());
        documents.addAll(loadJakartaDocs());

        ingest(documents);

        LOG.info("Data ingestion complete. " + documents.size() + " documents loaded.");
    }

    private List<Document> loadConferenceTalks() {
        List<ConferenceTalk> talks = em.createQuery("SELECT t FROM ConferenceTalk t", ConferenceTalk.class).getResultList();

        if (talks.isEmpty()) {
            seedDatabase();
            talks = em.createQuery("SELECT t FROM ConferenceTalk t", ConferenceTalk.class).getResultList();
        }

        return talks.stream()
                .map(talk -> Document.from(
                        "Talk: " + talk.getTitle() + "\n"
                                + "Speaker: " + talk.getSpeakerName() + " (" + talk.getSpeakerCompany() + ")\n"
                                + "Track: " + talk.getTrack() + "\n"
                                + "Time: " + talk.getTimeSlot() + "\n"
                                + "Abstract: " + talk.getAbstractText(),
                        new Metadata(Map.of("source", "conference-talk", "title", talk.getTitle()))
                ))
                .toList();
    }

    private List<Document> loadJakartaDocs() {
        List<Document> documents = new ArrayList<>();
        String[] docFiles = {"cdi.txt", "jpa.txt", "concurrency.txt", "data.txt", "jaxrs.txt"};

        DocumentParser parser = new TextDocumentParser();
        for (String filename : docFiles) {
            InputStream is = getClass().getResourceAsStream("/data/jakarta-docs/" + filename);
            if (is != null) {
                Document doc = parser.parse(is);
                documents.add(Document.from(
                        doc.text(),
                        new Metadata(Map.of("source", "jakarta-docs", "title", filename.replace(".txt", "")))
                ));
            }
        }
        return documents;
    }

    private void ingest(List<Document> documents) {
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documentSplitter(splitter)
                .build();

        ingestor.ingest(documents);
    }

    private void seedDatabase() {
        LOG.info("Seeding database with conference data...");

        Speaker speaker1 = new Speaker("Luqman Saeed",
                "Luqman is a Senior Software Engineer at Azul Systems with over 15 years of experience building enterprise Java applications. He specializes in Jakarta EE, virtual threads, and AI integration on the JVM.",
                "Azul", "Senior Software Engineer");
        Speaker speaker2 = new Speaker("Emily Chen",
                "Emily is a Principal Engineer at Red Hat working on Hibernate and Jakarta Persistence. She has contributed to multiple Jakarta EE specifications.",
                "Red Hat", "Principal Engineer");
        Speaker speaker3 = new Speaker("Marcus Weber",
                "Marcus leads the Payara Platform engineering team. He has been working with Java EE and Jakarta EE for over a decade, focusing on application server internals.",
                "Payara", "Head of Engineering");
        Speaker speaker4 = new Speaker("Sarah Johnson",
                "Sarah is a Developer Advocate at Eclipse Foundation, focusing on Jakarta EE adoption and community building.",
                "Eclipse Foundation", "Developer Advocate");
        Speaker speaker5 = new Speaker("David Kim",
                "David is a Staff Engineer at Oracle working on GraalVM and JVM performance. He leads the virtual threads performance optimization effort.",
                "Oracle", "Staff Engineer");

        em.persist(speaker1);
        em.persist(speaker2);
        em.persist(speaker3);
        em.persist(speaker4);
        em.persist(speaker5);

        em.persist(new ConferenceTalk(
                "The Intelligent Monolith: Supercharging Jakarta EE with Local AI",
                "Build a complete RAG system inside a Jakarta EE 11 application using local LLMs. See CDI seamlessly integrate open-source models for semantic search and intelligent responses. Transform JPA entities into vector embeddings, implement similarity search, and generate context-aware answers within your Java ecosystem.",
                "Luqman Saeed", "Azul",
                "Luqman is a Senior Software Engineer at Azul Systems with over 15 years of experience building enterprise Java applications.",
                "AI & ML", "Day 1, 14:00"
        ));

        em.persist(new ConferenceTalk(
                "Jakarta EE 11 Meets AI: Building Intelligent Microservices with Virtual Threads and Jakarta Data",
                "Develop a practical microservice entirely with Jakarta EE 11 that uses AI to answer questions directly from your documentation. Experience the power of virtual threads as they manage hundreds of concurrent AI requests without special configuration. Witness the simplicity of Jakarta Data as it treats vector embeddings like any other entity.",
                "Luqman Saeed", "Azul",
                "Luqman is a Senior Software Engineer at Azul Systems specializing in Jakarta EE and AI integration.",
                "AI & ML", "Day 2, 10:00"
        ));

        em.persist(new ConferenceTalk(
                "Virtual Threads in Production: Lessons from the Trenches",
                "After migrating a large-scale payment processing system to virtual threads, we learned what works, what doesn't, and where the pitfalls hide. This talk covers pinning issues, I/O patterns, observability with virtual threads, and performance comparisons with reactive frameworks.",
                "David Kim", "Oracle",
                "David is a Staff Engineer at Oracle working on GraalVM and JVM performance.",
                "Core Java", "Day 1, 11:00"
        ));

        em.persist(new ConferenceTalk(
                "Jakarta Persistence 3.2: What's New and Why It Matters",
                "Jakarta Persistence 3.2 brings significant improvements including better support for Java records, enhanced criteria API, and improved performance for batch operations. This session walks through the new features with live code examples and migration tips.",
                "Emily Chen", "Red Hat",
                "Emily is a Principal Engineer at Red Hat working on Hibernate and Jakarta Persistence.",
                "Persistence", "Day 1, 15:30"
        ));

        em.persist(new ConferenceTalk(
                "From Java EE to Jakarta EE: A Migration Story",
                "A real-world case study of migrating a 15-year-old Java EE 5 application to Jakarta EE 11. Covering namespace changes, build system migration, testing strategies, and the gotchas that cost us weeks of debugging.",
                "Marcus Weber", "Payara",
                "Marcus leads the Payara Platform engineering team.",
                "Migration", "Day 2, 14:00"
        ));

        em.persist(new ConferenceTalk(
                "Building Cloud-Native Jakarta EE Applications",
                "Learn how to structure Jakarta EE applications for cloud deployment using containers, health checks, and configuration management. Demonstrate deployment to Kubernetes with Payara Micro and show how MicroProfile specifications complement Jakarta EE.",
                "Marcus Weber", "Payara",
                "Marcus leads the Payara Platform engineering team.",
                "Cloud", "Day 1, 09:00"
        ));

        em.persist(new ConferenceTalk(
                "Jakarta Data 1.0: A New Era of Data Access",
                "Jakarta Data introduces a standardized repository abstraction for Java applications. See how to define repositories with minimal boilerplate, use the Jakarta Data Query Language, and leverage pagination for large datasets. Compare with existing approaches like JPA repositories and Spring Data.",
                "Emily Chen", "Red Hat",
                "Emily is a Principal Engineer at Red Hat.",
                "Persistence", "Day 2, 11:00"
        ));

        em.persist(new ConferenceTalk(
                "The State of Jakarta EE: 2026 and Beyond",
                "An overview of the Jakarta EE platform in 2026: what shipped in version 11, what's coming in version 12, and how the community is evolving. Includes roadmap discussion and audience Q&A.",
                "Sarah Johnson", "Eclipse Foundation",
                "Sarah is a Developer Advocate at Eclipse Foundation.",
                "Keynote", "Day 1, 09:00"
        ));

        LOG.info("Database seeded with 8 talks and 5 speakers.");
    }
}
