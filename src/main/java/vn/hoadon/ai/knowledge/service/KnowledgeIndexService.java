package vn.hoadon.ai.knowledge.service;

import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;
import vn.hoadon.ai.knowledge.config.KnowledgeProperties;
import vn.hoadon.ai.knowledge.model.KnowledgeIndexDocument;
import vn.hoadon.ai.knowledge.model.KnowledgeSearchHit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeIndexService {
    private final Analyzer analyzer = new VietnameseKnowledgeAnalyzer();
    private final Directory directory;

    public KnowledgeIndexService(KnowledgeProperties properties) {
        try {
            Path path = safeIndexPath(properties.getIndexPath());
            Files.createDirectories(path);
            directory = FSDirectory.open(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể mở thư mục chỉ mục kiến thức AI", ex);
        }
    }

    public synchronized void replaceDocument(long documentId, List<KnowledgeIndexDocument> chunks) {
        try (IndexWriter writer = writer(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)) {
            writer.deleteDocuments(new Term("document_id", Long.toString(documentId)));
            for (KnowledgeIndexDocument chunk : chunks) writer.addDocument(toLuceneDocument(chunk));
            writer.commit();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể cập nhật chỉ mục kiến thức AI", ex);
        }
    }

    public synchronized void deleteDocument(long documentId) {
        replaceDocument(documentId, List.of());
    }

    public synchronized void rebuild(List<KnowledgeIndexDocument> chunks) {
        try (IndexWriter writer = writer(IndexWriterConfig.OpenMode.CREATE)) {
            for (KnowledgeIndexDocument chunk : chunks) writer.addDocument(toLuceneDocument(chunk));
            writer.commit();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể xây dựng lại chỉ mục kiến thức AI", ex);
        }
    }

    public List<KnowledgeSearchHit> search(String text, Long companyId, boolean externalOnly, int limit) {
        if (text == null || text.isBlank() || limit < 1) return List.of();
        try {
            if (!DirectoryReader.indexExists(directory)) return List.of();
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                String escaped = MultiFieldQueryParser.escape(text.trim());
                QueryParser relevanceParser = new QueryParser("all", analyzer);
                relevanceParser.setDefaultOperator(QueryParser.Operator.OR);
                Query relevanceQuery = requireRelevantTerms(relevanceParser.parse(escaped));
                MultiFieldQueryParser boostParser = new MultiFieldQueryParser(
                        new String[]{"content", "title", "heading"}, analyzer,
                        Map.of("content", 1.0f, "title", 2.0f, "heading", 1.5f));
                boostParser.setDefaultOperator(QueryParser.Operator.OR);
                Query boostedQuery = boostParser.parse(escaped);
                Query textQuery = new BooleanQuery.Builder()
                        .add(relevanceQuery, BooleanClause.Occur.MUST)
                        .add(boostedQuery, BooleanClause.Occur.SHOULD)
                        .build();
                BooleanQuery.Builder query = new BooleanQuery.Builder().add(textQuery, BooleanClause.Occur.MUST);
                BooleanQuery.Builder scope = new BooleanQuery.Builder()
                        .add(new TermQuery(new Term("scope", "GLOBAL")), BooleanClause.Occur.SHOULD);
                if (companyId != null) {
                    scope.add(new TermQuery(new Term("company_id", Long.toString(companyId))), BooleanClause.Occur.SHOULD);
                }
                scope.setMinimumNumberShouldMatch(1);
                query.add(scope.build(), BooleanClause.Occur.FILTER);
                if (externalOnly) {
                    query.add(new TermQuery(new Term("external_share", "true")), BooleanClause.Occur.FILTER);
                }
                TopDocs topDocs = searcher.search(query.build(), Math.min(50, limit));
                List<KnowledgeSearchHit> result = new ArrayList<>(topDocs.scoreDocs.length);
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = searcher.storedFields().document(scoreDoc.doc);
                    result.add(new KnowledgeSearchHit(Long.parseLong(document.get("chunk_id")), scoreDoc.score));
                }
                return result;
            }
        } catch (ParseException ex) {
            return List.of();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể truy xuất chỉ mục kiến thức AI", ex);
        }
    }

    private Document toLuceneDocument(KnowledgeIndexDocument source) {
        Document document = new Document();
        document.add(new StringField("chunk_id", Long.toString(source.chunkId()), Field.Store.YES));
        document.add(new StringField("document_id", Long.toString(source.documentId()), Field.Store.NO));
        document.add(new StringField("version_id", Long.toString(source.versionId()), Field.Store.NO));
        document.add(new StringField("scope", source.companyId() == null ? "GLOBAL" : "COMPANY", Field.Store.NO));
        if (source.companyId() != null) {
            document.add(new StringField("company_id", Long.toString(source.companyId()), Field.Store.NO));
        }
        document.add(new StringField("external_share", Boolean.toString(source.externalShareAllowed()), Field.Store.NO));
        document.add(new TextField("title", value(source.title()), Field.Store.NO));
        document.add(new TextField("heading", value(source.heading()), Field.Store.NO));
        document.add(new TextField("content", value(source.content()), Field.Store.NO));
        document.add(new TextField("all", String.join("\n", value(source.title()), value(source.heading()),
                value(source.content())), Field.Store.NO));
        return document;
    }

    private IndexWriter writer(IndexWriterConfig.OpenMode mode) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(mode);
        config.setRAMBufferSizeMB(32);
        return new IndexWriter(directory, config);
    }

    private Query requireRelevantTerms(Query parsed) {
        if (!(parsed instanceof BooleanQuery booleanQuery)) return parsed;
        int optionalTerms = (int) booleanQuery.clauses().stream()
                .filter(clause -> clause.occur() == BooleanClause.Occur.SHOULD).count();
        if (optionalTerms < 2) return parsed;
        int minimum = optionalTerms <= 4 ? optionalTerms : Math.min(3, (optionalTerms + 1) / 2);
        BooleanQuery.Builder constrained = new BooleanQuery.Builder();
        booleanQuery.clauses().forEach(constrained::add);
        constrained.setMinimumNumberShouldMatch(minimum);
        return constrained.build();
    }

    private Path safeIndexPath(String configured) {
        if (configured == null || configured.isBlank()) throw new IllegalArgumentException("Thiếu đường dẫn chỉ mục kiến thức");
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (path.getParent() == null) throw new IllegalArgumentException("Đường dẫn chỉ mục kiến thức không an toàn");
        return path;
    }

    private String value(String value) { return value == null ? "" : value; }

    @PreDestroy
    void close() throws IOException {
        analyzer.close();
        directory.close();
    }
}
