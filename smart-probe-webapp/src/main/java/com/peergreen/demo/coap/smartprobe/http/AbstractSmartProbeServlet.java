/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 * Proprietary and confidential.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.peergreen.demo.coap.smartprobe.http;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * User: guillaume
 * Date: 30/08/13
 * Time: 12:14
 */
public abstract class AbstractSmartProbeServlet extends HttpServlet {

    public static final String SMARTPROBE_FACTORY = "com.peergreen.demo.coap.smartprobe.SmartProbe";

    protected ServiceTracker tracker;

    @Resource
    private BundleContext bundleContext;

    @Override
    public void init() throws ServletException {
        super.init();

        tracker = new ServiceTracker(bundleContext, Architecture.class.getName(), new ServiceTrackerCustomizer() {
            @Override
            public Object addingService(final ServiceReference reference) {
                Architecture arch = (Architecture) bundleContext.getService(reference);
                if (isSmartProbeInstance(arch)) {
                    return arch;
                }
                return null;
            }

            @Override
            public void modifiedService(final ServiceReference reference, final Object service) {

            }

            @Override
            public void removedService(final ServiceReference reference, final Object service) {
                bundleContext.ungetService(reference);
            }
        });

        tracker.open();

    }

    @Override
    public void destroy() {
        tracker.close();
        super.destroy();
    }

    private boolean isSmartProbeInstance(final Architecture arch) {
        ComponentInstance instance = arch.getInstanceDescription().getInstance();
        return SMARTPROBE_FACTORY.equals(instance.getFactory().getClassName());
    }
}
