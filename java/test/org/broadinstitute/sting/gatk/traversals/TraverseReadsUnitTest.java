package org.broadinstitute.sting.gatk.traversals;

import net.sf.picard.reference.ReferenceSequenceFile;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import org.broadinstitute.sting.BaseTest;
import org.broadinstitute.sting.gatk.ReadProperties;
import org.broadinstitute.sting.gatk.ReadMetrics;
import org.broadinstitute.sting.gatk.datasources.providers.ShardDataProvider;
import org.broadinstitute.sting.gatk.datasources.providers.ReadShardDataProvider;
import org.broadinstitute.sting.gatk.datasources.shards.Shard;
import org.broadinstitute.sting.gatk.datasources.shards.ShardStrategy;
import org.broadinstitute.sting.gatk.datasources.shards.ShardStrategyFactory;
import org.broadinstitute.sting.gatk.datasources.simpleDataSources.SAMDataSource;
import org.broadinstitute.sting.gatk.datasources.simpleDataSources.SAMReaderID;
import org.broadinstitute.sting.gatk.walkers.qc.CountReadsWalker;
import org.broadinstitute.sting.gatk.walkers.Walker;
import org.broadinstitute.sting.utils.GenomeLocParser;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 *
 * User: aaron
 * Date: Apr 24, 2009
 * Time: 3:42:16 PM
 *
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT 
 * This software and its documentation are copyright 2009 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 *
 */


/**
 * @author aaron
 * @version 1.0
 * @date Apr 24, 2009
 * <p/>
 * Class TraverseReadsUnitTest
 * <p/>
 * test traversing reads
 */
public class TraverseReadsUnitTest extends BaseTest {

    private ReferenceSequenceFile seq;
    private SAMReaderID bam = new SAMReaderID(new File(validationDataLocation + "index_test.bam"),Collections.<String>emptyList()); // TCGA-06-0188.aligned.duplicates_marked.bam");
    private File refFile = new File(validationDataLocation + "Homo_sapiens_assembly17.fasta");
    private List<SAMReaderID> bamList;
    private Walker countReadWalker;
    private File output;
    private long readSize = 100000;
    private TraverseReads traversalEngine = null;

    /**
     * This function does the setup of our parser, before each method call.
     * <p/>
     * Called before every test case method.
     */
    @Before
    public void doForEachTest() {
        output = new File("testOut.txt");
        FileOutputStream out = null;
        PrintStream ps; // declare a print stream object

        try {
            out = new FileOutputStream(output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            fail("Couldn't open the output file");
        }

        bamList = new ArrayList<SAMReaderID>();
        bamList.add(bam);
        countReadWalker = new CountReadsWalker();
        
        traversalEngine = new TraverseReads();


    }

    /** Test out that we can shard the file and iterate over every read */
    @Test
    public void testUnmappedReadCount() {
        IndexedFastaSequenceFile ref = null;
        ref = new IndexedFastaSequenceFile(refFile);
        GenomeLocParser.setupRefContigOrdering(ref);

        SAMDataSource dataSource = new SAMDataSource(new ReadProperties(bamList));
        ShardStrategy shardStrategy = ShardStrategyFactory.shatter(dataSource,ref,ShardStrategyFactory.SHATTER_STRATEGY.READS_EXPERIMENTAL,
                ref.getSequenceDictionary(),
                readSize);

        countReadWalker.initialize();
        Object accumulator = countReadWalker.reduceInit();

        while (shardStrategy.hasNext()) {
            Shard shard = shardStrategy.next();

            if (shard == null) {
                fail("Shard == null");
            }

            ShardDataProvider dataProvider = new ReadShardDataProvider(shard,dataSource.seek(shard),null,null);
            accumulator = traversalEngine.traverse(countReadWalker, dataProvider, accumulator);
            dataProvider.close();
        }

        traversalEngine.printOnTraversalDone(new ReadMetrics());
        countReadWalker.onTraversalDone(accumulator);

        if (!(accumulator instanceof Integer)) {
            fail("Count read walker should return an interger.");
        }
        if (((Integer) accumulator) != 10000) {
            fail("there should be 10000 mapped reads in the index file, there was " + ((Integer) accumulator));
        }
    }

}
