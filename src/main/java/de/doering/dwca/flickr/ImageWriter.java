package de.doering.dwca.flickr;

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.UnknownTerm;
import org.gbif.dwca.io.DwcaWriter;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageWriter {
  private Logger log = LoggerFactory.getLogger(getClass());
  private final DwcaWriter writer;
  private final Term thumbnail = new UnknownTerm(URI.create("http://flickr.com/terms/smallSquareUrl"),"smallSquareUrl");
  private final Term flickrid = new UnknownTerm(URI.create("http://flickr.com/terms/photoId"),"photoId");
  private final Cache<String, Integer> cache = CacheBuilder.newBuilder().maximumSize(10000).build();

  public ImageWriter(DwcaWriter writer) {
    this.writer = writer;
  }

  public boolean isNewImage(String id){
    if (cache.getIfPresent(id) == null) {
      return true;
    }
    return false;
  }

  public synchronized void writeImages(List<FlickrImage> images) throws IOException {
    long recordsAtStart = writer.getRecordsWritten();
    for (FlickrImage img : images){
      if (img == null) continue;
      // encountered this image before?
      if (!isNewImage(img.getId())){
        continue;
      }
      // remember weve seen the image
      cache.put(img.getId(), 1);

      writer.newRecord(img.getId());
      writer.addCoreColumn(DcTerm.source, img.getLink());
      writer.addCoreColumn(DwcTerm.scientificName, img.getScientificName());
      writer.addCoreColumn(DwcTerm.basisOfRecord, "HumanObservation");
      writer.addCoreColumn(DwcTerm.recordedBy, img.getPhotographer());

      // potentially null
      if (img.getDateRecorded()!=null){
        writer.addCoreColumn(DwcTerm.eventDate, img.getDateRecorded().toString());
      }
      if (img.getLongitude()!=null){
        writer.addCoreColumn(DwcTerm.decimalLongitude, img.getLongitude().toString());
      }
      if (img.getLatitude()!=null){
        writer.addCoreColumn(DwcTerm.decimalLatitude, img.getLatitude().toString());
      }
      if (img.getAccuracy()!=null){
        writer.addCoreColumn(DwcTerm.coordinatePrecision, img.getAccuracy().toString());
      }
      // additional, optional dynamic properties
      for (Term t : img.getAttributes().keySet()){
        writer.addCoreColumn(t, img.getAttribute(t));
      }

      // add image extension
      Map<Term, String> data = new HashMap<Term, String>();
      data.put(flickrid,img.getId());
      data.put(DcTerm.references, img.getLink());
      data.put(DcTerm.identifier, img.getImage());
      data.put(thumbnail,img.getThumb());
      data.put(DcTerm.license, img.getLicense());
      data.put(DcTerm.rightsHolder, img.getOwner());
      if (img.getDateRecorded()!=null){
        data.put(DcTerm.created, img.getDateRecorded().toString());
      }
      data.put(DcTerm.title, img.getTitle());
      data.put(DcTerm.description, img.getDescription());
      writer.addExtensionRecord(GbifTerm.Image, data);
    }
    log.debug("Written {} new images out of {} new candidates to archive with "+ writer.getRecordsWritten() +" records", (writer.getRecordsWritten()-recordsAtStart), images.size());
  }

  public static void main(String[] args) throws IOException {
    System.out.println(new Date().getTime());
  }

}
