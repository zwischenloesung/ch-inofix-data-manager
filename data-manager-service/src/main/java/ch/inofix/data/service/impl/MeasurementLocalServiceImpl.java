package ch.inofix.data.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.liferay.exportimport.kernel.controller.ExportController;
import com.liferay.exportimport.kernel.controller.ExportImportControllerRegistryUtil;
import com.liferay.exportimport.kernel.controller.ImportController;
import com.liferay.exportimport.kernel.model.ExportImportConfiguration;
import com.liferay.portal.kernel.backgroundtask.BackgroundTask;
import com.liferay.portal.kernel.backgroundtask.BackgroundTaskManagerUtil;
import com.liferay.portal.kernel.exception.LocaleException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.SystemEventConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.systemevent.SystemEvent;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import ch.inofix.data.background.task.MeasurementExportBackgroundTaskExecutor;
import ch.inofix.data.background.task.MeasurementImportBackgroundTaskExecutor;
import ch.inofix.data.constants.DataManagerField;
import ch.inofix.data.exception.MeasurementIdException;
import ch.inofix.data.exception.MeasurementNameException;
import ch.inofix.data.exception.MeasurementTimestampException;
import ch.inofix.data.exception.MeasurementValueException;
import ch.inofix.data.model.Measurement;
import ch.inofix.data.service.base.MeasurementLocalServiceBaseImpl;

/**
 * The implementation of the measurement local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are
 * added, rerun ServiceBuilder to copy their definitions into the
 * {@link ch.inofix.data.service.MeasurementLocalService} interface.
 *
 * <p>
 * This is a local service. Methods of this service will not have security
 * checks based on the propagated JAAS credentials because this service can only
 * be accessed from within the same VM.
 * </p>
 *
 * @author Christian Berndt
 * @created 2017-03-08 19:46
 * @modified 2018-01-04 12:15
 * @version 1.2.1
 * @see MeasurementLocalServiceBaseImpl
 * @see ch.inofix.data.service.MeasurementLocalServiceUtil
 */
public class MeasurementLocalServiceImpl
        extends MeasurementLocalServiceBaseImpl {
    /*
     * NOTE FOR DEVELOPERS:
     *
     * Never reference this class directly. Always use {@link
     * ch.inofix.data.service.MeasurementLocalServiceUtil} to access the
     * measurement local service.
     */

    @Indexable(type = IndexableType.REINDEX)
    @Override
    public Measurement addMeasurement(long userId, String data, String id,
            String name, Date timestamp, String unit, String value,
            ServiceContext serviceContext) throws PortalException {

        // Measurement

        User user = userLocalService.getUser(userId);

        validate(id, name, timestamp, value);

        long groupId = serviceContext.getScopeGroupId();

        long measurementId = counterLocalService.increment();

        Measurement measurement = measurementPersistence.create(measurementId);

        measurement.setUuid(serviceContext.getUuid());
        measurement.setGroupId(groupId);
        measurement.setCompanyId(user.getCompanyId());
        measurement.setUserId(user.getUserId());
        measurement.setUserName(user.getFullName());
        measurement.setExpandoBridgeAttributes(serviceContext);

        measurement.setData(data);
        measurement.setId(id);
        measurement.setName(name);
        measurement.setTimestamp(timestamp);
        measurement.setUnit(unit);
        measurement.setValue(value);

        measurementPersistence.update(measurement);

        // Resources

        resourceLocalService.addModelResources(measurement, serviceContext);

        // Asset

        updateAsset(userId, measurement, serviceContext.getAssetCategoryIds(),
                serviceContext.getAssetTagNames(),
                serviceContext.getAssetPriority());

        return measurement;

    }

    @Override
    public List<Measurement> deleteGroupMeasurements(long groupId)
            throws PortalException {

        List<Measurement> measurements = measurementPersistence
                .findByGroupId(groupId);

        for (Measurement measurement : measurements) {
            deleteMeasurement(measurement);
        }

        return measurements;

    }

    @Indexable(type = IndexableType.DELETE)
    @Override
    @SystemEvent(type = SystemEventConstants.TYPE_DELETE)
    public Measurement deleteMeasurement(Measurement measurement)
            throws PortalException {

        // Measurement

        measurementPersistence.remove(measurement);

        // Resources

        resourceLocalService.deleteResource(measurement.getCompanyId(),
                Measurement.class.getName(), ResourceConstants.SCOPE_INDIVIDUAL,
                measurement.getMeasurementId());

        return measurement;
    }

    @Override
    public Measurement deleteMeasurement(long measurementId)
            throws PortalException {

        Measurement measurement = measurementPersistence
                .findByPrimaryKey(measurementId);

        return measurementLocalService.deleteMeasurement(measurement);
    }

    @Override
    public File exportMeasurementsAsFile(
            ExportImportConfiguration exportImportConfiguration)
            throws PortalException {

        try {
            ExportController measurementExportController = ExportImportControllerRegistryUtil
                    .getExportController(Measurement.class.getName());

            return measurementExportController
                    .export(exportImportConfiguration);

        } catch (PortalException pe) {
            _log.error(pe);
            throw pe;
        } catch (Exception e) {
            _log.error(e);
            throw new SystemException(e);
        }
    }

    @Override
    public long exportMeasurementsAsFileInBackground(long userId,
            ExportImportConfiguration exportImportConfiguration)
            throws PortalException {

        // TODO: The export may have different file types / extensions:
        // - .csv
        // - .xml
        // - .txt
        // - .json
        // if
        // (!DLValidatorUtil.isValidName(exportImportConfiguration.getName())) {
        // throw new LARFileNameException(exportImportConfiguration.getName());
        // }

        Map<String, Serializable> taskContextMap = new HashMap<>();

        taskContextMap.put("exportImportConfigurationId",
                exportImportConfiguration.getExportImportConfigurationId());

        BackgroundTask backgroundTask = BackgroundTaskManagerUtil
                .addBackgroundTask(userId,
                        exportImportConfiguration.getGroupId(),
                        exportImportConfiguration.getName(),
                        MeasurementExportBackgroundTaskExecutor.class.getName(),
                        taskContextMap, new ServiceContext());

        return backgroundTask.getBackgroundTaskId();
    }

    @Override
    public void importMeasurements(
            ExportImportConfiguration exportImportConfiguration, File file)
            throws PortalException {

        _log.info("importMeasurements()");

        _log.info("file = " + file);

        try {
            ImportController measurementImportController = ExportImportControllerRegistryUtil
                    .getImportController(Measurement.class.getName());

            measurementImportController.importFile(exportImportConfiguration,
                    file);

            _log.info("measurementImportController = "
                    + measurementImportController);

        } catch (PortalException pe) {
            Throwable cause = pe.getCause();

            if (cause instanceof LocaleException) {
                throw (PortalException) cause;
            }

            throw pe;
        } catch (SystemException se) {
            throw se;
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void importMeasurements(
            ExportImportConfiguration exportImportConfiguration,
            InputStream inputStream) throws PortalException {

        _log.info("importMeasurements()");

        File file = null;

        try {

            // TODO: use extension from upload
            file = FileUtil.createTempFile("lar");

            FileUtil.write(file, inputStream);

            importMeasurements(exportImportConfiguration, file);

        } catch (IOException ioe) {
            throw new SystemException(ioe);
        } finally {
            FileUtil.delete(file);
        }
    }

    @Override
    public long importMeasurementsInBackground(long userId,
            ExportImportConfiguration exportImportConfiguration, File file)
            throws PortalException {

        _log.info("importMeasurementsInBackground()");

        Map<String, Serializable> taskContextMap = new HashMap<>();

        taskContextMap.put("exportImportConfigurationId",
                exportImportConfiguration.getExportImportConfigurationId());

        BackgroundTask backgroundTask = BackgroundTaskManagerUtil
                .addBackgroundTask(userId,
                        exportImportConfiguration.getGroupId(),
                        exportImportConfiguration.getName(),
                        MeasurementImportBackgroundTaskExecutor.class.getName(),
                        taskContextMap, new ServiceContext());

        backgroundTask.addAttachment(userId, file.getName(), file);

        return backgroundTask.getBackgroundTaskId();
    }

    @Override
    public long importMeasurementsInBackground(long userId,
            ExportImportConfiguration exportImportConfiguration,
            InputStream inputStream, String extension) throws PortalException {

        _log.info("importMeasurementsInBackground()");

        File file = null;

        try {

            file = FileUtil.createTempFile(extension);

            FileUtil.write(file, inputStream);

            return importMeasurementsInBackground(userId,
                    exportImportConfiguration, file);

        } catch (IOException ioe) {
            throw new SystemException(ioe);
        } finally {
            FileUtil.delete(file);
        }
    }

    @Override
    public Hits search(long userId, long groupId, String keywords, int start,
            int end, Sort sort) throws PortalException {

        if (sort == null) {
            sort = new Sort(Field.MODIFIED_DATE, true);
        }

        String data = null;
        String id = null;
        String name = null;

        boolean andOperator = false;

        if (Validator.isNotNull(keywords)) {

            data = keywords;
            id = keywords;
            name = keywords;

        } else {
            andOperator = true;
        }

        return search(userId, groupId, data, id, name, null, null, null, null,
                null, andOperator, start, end, sort);

    }

    @Override
    public Hits search(long userId, long groupId, String data, String id,
            String name, String range, Date timestamp, Date from, Date until,
            LinkedHashMap<String, Object> params, boolean andSearch, int start,
            int end, Sort sort) throws PortalException {

        if (sort == null) {
            sort = new Sort(DataManagerField.TIMESTAMP, true);
        }

        Indexer<Measurement> indexer = IndexerRegistryUtil
                .getIndexer(Measurement.class.getName());

        SearchContext searchContext = buildSearchContext(userId, groupId, data,
                id, name, range, timestamp, from, until, params, andSearch,
                start, end, sort);

        return indexer.search(searchContext);

    }

    @Override
    public void updateAsset(long userId, Measurement measurement,
            long[] assetCategoryIds, String[] assetTagNames, Double priority)
            throws PortalException {

        // TODO
        boolean visible = true;
        // boolean visible = false;
        // if (measurement.isApproved()) {
        // visible = true;
        // publishDate = measurement.getCreateDate();
        // }

        Date publishDate = null;

        String summary = HtmlUtil
                .extractText(StringUtil.shorten(measurement.getData(), 500));

        String className = Measurement.class.getName();
        long classPK = measurement.getMeasurementId();

        assetEntryLocalService.updateEntry(userId, measurement.getGroupId(),
                measurement.getCreateDate(), measurement.getModifiedDate(),
                className, classPK, measurement.getUuid(), 0, assetCategoryIds,
                assetTagNames, true, visible, null, null, publishDate, null,
                ContentTypes.TEXT_HTML, measurement.getName(),
                measurement.getName(), summary, null, null, 0, 0, priority);
    }

    @Indexable(type = IndexableType.REINDEX)
    @Override
    public Measurement updateMeasurement(long measurementId, long userId,
            String data, String id, String name, Date timestamp, String unit,
            String value, ServiceContext serviceContext)
            throws PortalException {

        // Measurement

        Measurement measurement = measurementPersistence
                .findByPrimaryKey(measurementId);

        validate(id, name, timestamp, value);

        measurement.setExpandoBridgeAttributes(serviceContext);

        measurement.setData(data);
        measurement.setId(id);
        measurement.setName(name);
        measurement.setTimestamp(timestamp);
        measurement.setUnit(unit);
        measurement.setValue(value);

        measurementPersistence.update(measurement);

        // Asset

        updateAsset(userId, measurement, serviceContext.getAssetCategoryIds(),
                serviceContext.getAssetTagNames(),
                serviceContext.getAssetPriority());

        return measurement;

    }

    protected SearchContext buildSearchContext(long userId, long groupId,
            String data, String id, String name, String range, Date timestamp,
            Date fromDate, Date untilDate, LinkedHashMap<String, Object> params,
            boolean andSearch, int start, int end, Sort sort)
            throws PortalException {

        SearchContext searchContext = new SearchContext();

        searchContext.setAttribute("advancedSearch", true);

        searchContext.setAttribute(Field.STATUS, WorkflowConstants.STATUS_ANY);

        if (Validator.isNotNull(data)) {
            searchContext.setAttribute("data", data);
        }

        if (Validator.isNotNull(id)) {
            searchContext.setAttribute("id", id);
        }

        if (Validator.isNotNull(name)) {
            searchContext.setAttribute("name", name);
        }

        if (Validator.isNotNull(range)) {
            searchContext.setAttribute("range", range);
        }

        if (Validator.isNotNull(timestamp)) {
            searchContext.setAttribute("timestamp", timestamp.getTime());
        }

        if (Validator.isNotNull(fromDate)) {
            searchContext.setAttribute("fromDate", fromDate);
        }

        if (Validator.isNotNull(untilDate)) {
            searchContext.setAttribute("untilDate", untilDate);
        }

        searchContext.setAttribute("paginationType", "more");

        Group group = GroupLocalServiceUtil.getGroup(groupId);

        searchContext.setCompanyId(group.getCompanyId());

        searchContext.setEnd(end);
        if (groupId > 0) {
            searchContext.setGroupIds(new long[] { groupId });
        }
        searchContext.setSorts(sort);
        searchContext.setStart(start);
        searchContext.setUserId(userId);

        searchContext.setAndSearch(andSearch);

        if (params != null) {

            String keywords = (String) params.remove("keywords");

            if (Validator.isNotNull(keywords)) {
                searchContext.setKeywords(keywords);
            }
        }

        QueryConfig queryConfig = new QueryConfig();

        queryConfig.setHighlightEnabled(false);
        queryConfig.setScoreEnabled(false);

        searchContext.setQueryConfig(queryConfig);

        if (sort != null) {
            searchContext.setSorts(sort);
        }

        searchContext.setStart(start);

        return searchContext;
    }

    protected void validate(String id, String name, Date timestamp,
            String value) throws PortalException {

        if (Validator.isNull(id)) {
            throw new MeasurementIdException();
        }

        if (Validator.isNull(name)) {
            throw new MeasurementNameException();
        }

        if (Validator.isNull(timestamp)) {
            throw new MeasurementTimestampException();
        }

        if (Validator.isNull(value)) {
            throw new MeasurementValueException();
        }
    }

    private static final Log _log = LogFactoryUtil
            .getLog(MeasurementLocalServiceImpl.class.getName());

}
