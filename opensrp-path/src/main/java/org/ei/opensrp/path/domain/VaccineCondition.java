package org.ei.opensrp.path.domain;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.commonregistry.AllCommonsRepository;
import org.ei.opensrp.domain.Vaccine;
import org.ei.opensrp.path.application.VaccinatorApplication;
import org.ei.opensrp.path.db.VaccineRepo;
import org.ei.opensrp.repository.DetailsRepository;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import util.Utils;

/**
 * Created by Jason Rogena - jrogena@ona.io on 19/05/2017.
 */

public abstract class VaccineCondition {
    protected final VaccineRepo.Vaccine vaccine;
    public static final String TYPE_GIVEN = "given";
    public static final String TYPE_NOT_GIVEN = "not_given";

    public VaccineCondition(VaccineRepo.Vaccine vaccine) {
        this.vaccine = vaccine;
    }

    public static VaccineCondition init(String vaccineCategory, JSONObject conditionData) throws JSONException {
        if (conditionData.has("type")) {
            if (conditionData.getString("type").equals(TYPE_GIVEN)) {
                GivenCondition.Comparison comparison = GivenCondition.getComparison(conditionData.getString("comparison"));
                VaccineRepo.Vaccine vaccine = VaccineRepo.getVaccine(conditionData.getString("vaccine"),
                        vaccineCategory);

                if (comparison != null && vaccine != null) {
                    return new GivenCondition(vaccine, conditionData.getString("value"), comparison);
                }
            } else if (conditionData.getString("type").equals(TYPE_NOT_GIVEN)) {
                VaccineRepo.Vaccine vaccine = VaccineRepo.getVaccine(conditionData.getString("vaccine"),
                        vaccineCategory);

                if (vaccine != null) {
                    return new NotGivenCondition(vaccine);
                }
            }
        } else if (conditionData.has("attribute") && conditionData.has("value")) {
            String attribute = conditionData.getString("attribute");
            String value = conditionData.getString("value");

            return new AttributeCondition(null, attribute, value);
        }

        return null;
    }

    public abstract boolean passes(List<Vaccine> issuedVaccines);

    public static class NotGivenCondition extends VaccineCondition {

        public NotGivenCondition(VaccineRepo.Vaccine vaccine) {
            super(vaccine);
        }

        @Override
        public boolean passes(List<Vaccine> issuedVaccines) {
            // Check if vaccine was not given
            boolean given = false;

            // TODO: Check if name used in VaccineRepo.Vaccine is the same as the one in Vaccine
            for (Vaccine curVaccine : issuedVaccines) {
                if (curVaccine.getName().equalsIgnoreCase(vaccine.display())) {
                    given = true;
                    break;
                }
            }

            return !given;
        }
    }

    public static class AttributeCondition extends VaccineCondition{

        String baseEntityId;
        String attribute;
        String value;

        public AttributeCondition(VaccineRepo.Vaccine vaccine, String attribute, String value) {
            super(vaccine);
            this.attribute = attribute;
            this.value = value;
        }

        public boolean passes(String baseEntityId) {
            String tableName = "ec_child";
            VaccinatorApplication vaccinatorApplication = VaccinatorApplication.getInstance();
            AllCommonsRepository allCommonsRepository = vaccinatorApplication.context().allCommonsRepositoryobjects(tableName);
            Map<String, String> columnMaps = allCommonsRepository.findByCaseID(baseEntityId).getColumnmaps();
            DetailsRepository detailsRepository = vaccinatorApplication.context().detailsRepository();
            Map<String, String> details = detailsRepository.getAllDetailsForClient(baseEntityId);

            String childAttributeValue = columnMaps.get(this.attribute);
            if(childAttributeValue == null){
                childAttributeValue = details.get(this.attribute);
            }
            
            if(childAttributeValue.equalsIgnoreCase(this.value)){
                return true;
            }

            return false;
        }

        @Override
        public boolean passes(List<Vaccine> issuedVaccines) {
            return passes(this.baseEntityId);
        }

        public String getBaseEntityId() {
            return baseEntityId;
        }

        public void setBaseEntityId(String baseEntityId) {
            this.baseEntityId = baseEntityId;
        }
    }

    public static class GivenCondition extends VaccineCondition {
        public static enum Comparison {
            EXACTLY("exactly"),
            AT_LEAST("at_least"),
            AT_MOST("at_most");

            private final String name;

            Comparison(String name) {
                this.name = name;
            }
        }

        public static Comparison getComparison(String name) {
            for (Comparison curComparison : Comparison.values()) {
                if (curComparison.name.equalsIgnoreCase(name)) {
                    return curComparison;
                }
            }

            return null;
        }

        private final Comparison comparison;
        private final String value;

        public GivenCondition(VaccineRepo.Vaccine vaccine, String value, Comparison comparison) {
            super(vaccine);
            this.value = value;
            this.comparison = comparison;
        }

        @Override
        public boolean passes(List<Vaccine> issuedVaccines) {
            boolean result = false;

            // Check if vaccine was given at all
            Vaccine comparisonVaccine = null;
            for (Vaccine curVaccine : issuedVaccines) {
                if (curVaccine.getName().equalsIgnoreCase(vaccine.display())) {
                    comparisonVaccine = curVaccine;
                    break;
                }
            }

            if (comparisonVaccine != null) {
                Calendar comparisonDate = Calendar.getInstance();
                VaccineSchedule.standardiseCalendarDate(comparisonDate);
                comparisonDate = VaccineSchedule.addOffsetToCalendar(comparisonDate, value);

                Calendar vaccinationDate = Calendar.getInstance();
                vaccinationDate.setTime(comparisonVaccine.getDate());
                VaccineSchedule.standardiseCalendarDate(vaccinationDate);

                switch (comparison) {
                    case EXACTLY:
                        result = comparisonDate.getTimeInMillis() == vaccinationDate.getTimeInMillis();
                        break;
                    case AT_LEAST:
                        result = vaccinationDate.getTimeInMillis() >= comparisonDate.getTimeInMillis();
                        break;
                    case AT_MOST:
                        result = vaccinationDate.getTimeInMillis() <= comparisonDate.getTimeInMillis();
                        break;
                }
            }

            return result;
        }
    }
}
