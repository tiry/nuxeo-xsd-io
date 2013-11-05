package org.nuxeo;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = MyRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.xsd.io")
@LocalDeploy("org.nuxeo.xsd.io:OSGI-INF/doc-type-contrib.xml")
public class TestImportExportWithComplexXSD {

    @Inject
    protected CoreSession session;

    @Test
    public void checkComplexTypeExportImport() throws Exception  {
        DocumentModel doc = session.getDocument(new PathRef("/testDoc"));
        Assert.assertNotNull(doc);

        verifyProperties(doc);
        File out = File.createTempFile("model-io", ".zip");

        DocumentReader reader = new SingleDocumentReader(session, doc);
        DocumentWriter writer = new NuxeoArchiveWriter(out);

        try {
            // creating a pipe
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        }
        finally {
            writer.close();
            reader.close();
        }

        Assert.assertTrue(out.length()>0);

        DocumentModel newParent = session.createDocumentModel("/", "importRoot", "Folder");
        newParent.setPropertyValue("dc:title", "Import Root");
        newParent = session.createDocument(newParent);
        session.save();

        reader = new NuxeoArchiveReader(out);
        writer = new DocumentModelWriter(session, newParent.getPathAsString());

        try {
            // creating a pipe
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        }
        finally {
            writer.close();
            reader.close();
        }

        DocumentModelList children = session.getChildren(newParent.getRef());
        Assert.assertEquals(1, children.totalSize());
        verifyProperties(children.get(0));
    }

    protected void verifyProperties(DocumentModel doc) throws Exception {
        Assert.assertEquals("Insurance", doc.getPropertyValue("mc:modelType"));

        Assert.assertEquals("V1", doc.getPropertyValue("mc:currentVersion"));
        Assert.assertEquals("Internal", doc.getPropertyValue("mc:origin"));

        Map<String, Serializable> segment = (Map<String, Serializable>) doc.getPropertyValue("mc:segmentVariable");

        Assert.assertEquals("MySegment", segment.get("name"));
        Assert.assertEquals("SomeTarget", segment.get("target"));
        Assert.assertEquals("rawVariable", segment.get("variableType"));

        List<String> roles = (List<String>) segment.get("roles");

        Assert.assertEquals(3, roles.size());
        Assert.assertTrue(roles.contains("Score"));
        Assert.assertTrue(roles.contains("ComparisonScore"));
        Assert.assertTrue(roles.contains("Decision"));
    }



}
