package playground.sebhoerl.recharging_avs;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.jfree.data.io.CSV;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import playground.sebhoerl.av_paper.BinCalculator;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.recharging_avs.calculators.ChargeCalculator;
import playground.sebhoerl.recharging_avs.calculators.StaticChargeCalculator;
import playground.sebhoerl.recharging_avs.logic.RechargingDispatcher;
import playground.sebhoerl.recharging_avs.tracker.CSVConsumptionTracker;
import playground.sebhoerl.recharging_avs.tracker.ConsumptionTracker;
import playground.sebhoerl.recharging_avs.tracker.NullConsumptionTracker;

public class RechargingModule extends AbstractModule {
    final private Logger logger = Logger.getLogger(RechargingModule.class);

    @Override
    public void install() {
        bind(ChargeCalculator.class).to(StaticChargeCalculator.class).asEagerSingleton();
        AVUtils.registerDispatcherFactory(binder(), "Recharging", RechargingDispatcher.Factory.class);

        RechargingConfig config = (RechargingConfig)getConfig().getModules().get(RechargingConfig.RECHARGING);

        if (config.getTrackConsumption()) {
            logger.info("EAV Consumption tracking enabled");
            bind(ConsumptionTracker.class).to(CSVConsumptionTracker.class);
            addControlerListenerBinding().to(CSVConsumptionTracker.class);
        } else {
            logger.info("EAV Consumption tracking disabled");
            bind(ConsumptionTracker.class).toInstance(new NullConsumptionTracker());
        }
    }

    @Provides @Singleton
    private CSVConsumptionTracker provideCSVConsumptionTracker(RechargingConfig rechargingConfig) {
        if (rechargingConfig.getTrackingEndTime() <= rechargingConfig.getTrackingStartTime()) {
            throw new IllegalArgumentException("Tracking end time must be larger than start time");
        }

        if (!((rechargingConfig.getTrackingEndTime() - rechargingConfig.getTrackingStartTime()) % rechargingConfig.getTrackingBinDuration() == 0)) {
            throw new IllegalArgumentException();
        }

        BinCalculator binCalculator = BinCalculator.createByInterval(rechargingConfig.getTrackingStartTime(), rechargingConfig.getTrackingEndTime(), rechargingConfig.getTrackingBinDuration());
        return new CSVConsumptionTracker(binCalculator);
    }
}
