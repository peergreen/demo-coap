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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;

/**
 * User: guillaume
 * Date: 29/08/13
 * Time: 14:09
 */
@WebServlet("/configure")
public class ConfigServlet extends AbstractSmartProbeServlet {

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doIt(req, resp);
    }

    private void doIt(final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
        Architecture architecture = (Architecture) tracker.getService();
        if (architecture == null) {
            throw new ServletException("SmartProbe instance not available on the system");
        }

        ComponentInstance instance = architecture.getInstanceDescription().getInstance();
        String interval = req.getParameter("interval");
        if (!isNullOrEmpty(interval)) {
            resp.getWriter().printf("Interval changed to %s ms. ", interval);
            instance.reconfigure(newConfiguration("interval", interval));
        }

        String uri = req.getParameter("uri");
        if (!isNullOrEmpty(uri)) {
            resp.getWriter().printf("URI changed to %s. ", uri);
            instance.reconfigure(newConfiguration("smarthing-uri", uri));
        }
    }

    private boolean isNullOrEmpty(final String value) {
        return (value == null) || ("".equals(value));
    }

    private static Dictionary<String, Object> newConfiguration(final String name, final Object value) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(name, value);
        return properties;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doIt(req, resp);
    }
}
