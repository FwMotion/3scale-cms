package com.fwmotion.threescale.cms.cli.support;

import com.fwmotion.threescale.cms.model.CmsObject;
import com.fwmotion.threescale.cms.model.ThreescaleObjectType;

import javax.enterprise.context.ApplicationScoped;
import java.util.Comparator;

@ApplicationScoped
public class CmsSectionToTopComparator implements Comparator<CmsObject> {

    @Override
    public int compare(CmsObject left, CmsObject right) {
        boolean leftIsSection = left.getType() == ThreescaleObjectType.SECTION;
        boolean rightIsSection = right.getType() == ThreescaleObjectType.SECTION;
        if (leftIsSection) {
            if (rightIsSection) {
                return 0;
            }
            return -1;
        } else if (rightIsSection) {
            return 1;
        }

        return 0;
    }

}
